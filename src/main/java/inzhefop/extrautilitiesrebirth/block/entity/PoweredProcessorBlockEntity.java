package inzhefop.extrautilitiesrebirth.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nullable;
import java.util.stream.IntStream;

public abstract class PoweredProcessorBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    private static final String RECIPE_TAG = "ProcessingRecipe";
    private static final String DURATION_TAG = "progresstime";

    private final String progressTag;
    private final Component defaultName;
    private final Component displayName;
    private final int[] slots;
    private NonNullList<ItemStack> items;

    private final EnergyStorage storage;
    private LazyOptional<EnergyStorage> energy;
    private LazyOptional<? extends IItemHandler>[] sidedItems;

    protected PoweredProcessorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                          int slots, int capacity, String progressTag, String defaultName, String displayName) {
        super(type, pos, state);
        this.progressTag = progressTag;
        this.defaultName = Component.literal(defaultName);
        this.displayName = Component.literal(displayName);
        this.slots = IntStream.range(0, slots).toArray();
        this.items = NonNullList.withSize(slots, ItemStack.EMPTY);
        this.storage = new EnergyStorage(capacity) {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                int received = super.receiveEnergy(maxReceive, simulate);
                if (received > 0 && !simulate) {
                    syncEnergy();
                }
                return received;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                int extracted = super.extractEnergy(maxExtract, simulate);
                if (extracted > 0 && !simulate) {
                    syncEnergy();
                }
                return extracted;
            }
        };
        energy = LazyOptional.of(() -> storage);
        sidedItems = SidedInvWrapper.create(this, Direction.values());
    }

    @Nullable
    protected abstract Operation operation(int upgrades);

    protected abstract boolean acceptsIngredient(int slot, ItemStack stack);

    public final void processTick() {
        int upgrades = canPlaceItem(upgradeSlot(), getItem(upgradeSlot())) ? getItem(upgradeSlot()).getCount() : 0;
        Operation operation = operation(upgrades);
        CompoundTag data = getPersistentData();
        if (operation == null) {
            if (data.contains(RECIPE_TAG) || data.getDouble(progressTag) != 0 || data.getInt(DURATION_TAG) != 0) {
                data.remove(RECIPE_TAG);
                data.putDouble(progressTag, 0);
                data.putDouble(DURATION_TAG, 0);
                setChanged();
            }
            return;
        }

        String recipeId = operation.id();
        if (!recipeId.equals(data.getString(RECIPE_TAG))) {
            // An older save recorded progress without a recipe ID.
            if (data.contains(RECIPE_TAG)) {
                data.putDouble(progressTag, 0);
            }
            data.putString(RECIPE_TAG, recipeId);
            setChanged();
        }
        if (data.getInt(DURATION_TAG) != operation.duration()) {
            data.putDouble(DURATION_TAG, operation.duration());
            setChanged();
        }

        ItemStack result = operation.result();
        ItemStack stored = getItem(outputSlot());
        int limit = Math.min(result.getMaxStackSize(), getMaxStackSize());
        if (getItem(0).getCount() < operation.inputCount()
                || (operation.reagentCount() > 0 && getItem(1).getCount() < operation.reagentCount())
                || (!stored.isEmpty() && !ItemStack.isSameItemSameTags(stored, result))
                || result.getCount() > limit - stored.getCount()
                || storage.extractEnergy(operation.energy(), true) != operation.energy()) {
            return;
        }

        double progress = Math.max(0, data.getDouble(progressTag)) + operation.workPerTick();
        if (progress < operation.duration()) {
            data.putDouble(progressTag, progress);
            setChanged();
            return;
        }

        removeItem(0, operation.inputCount());
        if (operation.reagentCount() > 0) {
            removeItem(1, operation.reagentCount());
        }
        setItem(outputSlot(), stored.isEmpty() ? result.copy() : stored.copyWithCount(stored.getCount() + result.getCount()));
        data.putDouble(progressTag, 0);
        storage.extractEnergy(operation.energy(), false);
    }

    public final int progressPixels() {
        CompoundTag data = getPersistentData();
        double duration = data.getDouble(DURATION_TAG);
        return duration <= 0 ? 0 : Mth.clamp((int) (22 * data.getDouble(progressTag) / duration), 0, 22);
    }

    public final int energyPixels() {
        return Mth.clamp((int) (70L * storage.getEnergyStored() / storage.getMaxEnergyStored()), 0, 70);
    }

    protected final int energyCapacity() {
        return storage.getMaxEnergyStored();
    }

    public final int upgradeSlot() {
        return slots.length - 1;
    }

    public final int outputSlot() {
        return slots.length - 2;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items = NonNullList.withSize(slots.length, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            setLootTable(null, 0);
            ContainerHelper.loadAllItems(tag, items);
        }
        storage.deserializeNBT(tag.get("energyStorage") instanceof IntTag savedEnergy ? savedEnergy : IntTag.valueOf(0));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, items);
        }
        tag.put("energyStorage", storage.serializeNBT());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithFullMetadata();
    }

    @Override
    public int getContainerSize() {
        return slots.length;
    }

    @Override
    public Component getDefaultName() {
        return defaultName;
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? getName() : displayName;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (stack.isEmpty() || slot < 0 || slot >= slots.length) {
            return false;
        }
        return slot == upgradeSlot() ? MachineSpeed.factorOf(stack) > 0
                : slot < outputSlot() && acceptsIngredient(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return slots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == outputSlot();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (!isRemoved()) {
            if (capability == ForgeCapabilities.ITEM_HANDLER && side != null) {
                return sidedItems[side.ordinal()].cast();
            }
            if (capability == ForgeCapabilities.ENERGY) {
                return energy.cast();
            }
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (var handler : sidedItems) {
            handler.invalidate();
        }
        energy.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        sidedItems = SidedInvWrapper.create(this, Direction.values());
        energy = LazyOptional.of(() -> storage);
    }

    private void syncEnergy() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    protected record Operation(String id, ItemStack result, int inputCount, int reagentCount, int energy, int duration,
                               double workPerTick) {
    }
}
