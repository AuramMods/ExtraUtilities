package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.world.inventory.NormalGUIMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
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

public abstract class FuelGeneratorBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    private static final int FUEL_SLOT = 0;
    private static final int UPGRADE_SLOT = 1;
    private static final int[] SLOTS = {FUEL_SLOT, UPGRADE_SLOT};

    private final GeneratorFuel fuel;
    private final Component defaultName;
    private final Component displayName;
    private NonNullList<ItemStack> stacks = NonNullList.withSize(SLOTS.length, ItemStack.EMPTY);
    private LazyOptional<? extends IItemHandler>[] sidedItems = SidedInvWrapper.create(this, Direction.values());
    private final EnergyStorage storage = new EnergyStorage(100_000) {
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
    private LazyOptional<EnergyStorage> energy = LazyOptional.of(() -> storage);
    private LazyOptional<GeneratorEnergyOutput> output = LazyOptional.of(() -> new GeneratorEnergyOutput(storage));

    protected FuelGeneratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                       GeneratorFuel fuel, String defaultName, String displayName) {
        super(type, pos, state);
        this.fuel = fuel;
        this.defaultName = Component.literal(defaultName);
        this.displayName = Component.literal(displayName);
    }

    public GeneratorFuel getFuelType() {
        return fuel;
    }

    public ItemStack getFuelStack() {
        return getItem(FUEL_SLOT);
    }

    public double getSpeedMultiplier() {
        return GeneratorSpeed.multiplier(getItem(UPGRADE_SLOT));
    }

    public void consumeFuel() {
        ItemStack consumed = removeItem(FUEL_SLOT, 1);
        ItemStack remainder = fuel.remainder(consumed);
        if (remainder.isEmpty()) {
            return;
        }
        if (getFuelStack().isEmpty()) {
            setItem(FUEL_SLOT, remainder);
        } else {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stacks = NonNullList.withSize(SLOTS.length, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            setLootTable(null, 0);
            ContainerHelper.loadAllItems(tag, stacks);
        }
        if (tag.get("energyStorage") instanceof IntTag savedEnergy) {
            storage.deserializeNBT(savedEnergy);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, stacks);
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
        return stacks.size();
    }

    @Override
    public Component getDefaultName() {
        return defaultName;
    }

    @Override
    public Component getDisplayName() {
        return displayName;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new NormalGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(worldPosition));
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return stacks;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return switch (index) {
            case FUEL_SLOT -> fuel.burnDuration(stack) > 0;
            case UPGRADE_SLOT -> GeneratorSpeed.multiplier(stack) > 1;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(index, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction side) {
        return index == FUEL_SLOT && fuel.burnDuration(stack) <= 0;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (!isRemoved()) {
            if (capability == ForgeCapabilities.ITEM_HANDLER && side != null) {
                return sidedItems[side.ordinal()].cast();
            }
            if (capability == ForgeCapabilities.ENERGY) {
                return side == null ? energy.cast() : output.cast();
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
        output.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        sidedItems = SidedInvWrapper.create(this, Direction.values());
        energy = LazyOptional.of(() -> storage);
        output = LazyOptional.of(() -> new GeneratorEnergyOutput(storage));
    }

    private void syncEnergy() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }
}
