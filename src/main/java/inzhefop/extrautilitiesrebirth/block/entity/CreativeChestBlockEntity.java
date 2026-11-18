package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import inzhefop.extrautilitiesrebirth.world.inventory.CreativeChestGUIMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

import javax.annotation.Nullable;

public class CreativeChestBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    public static final int TEMPLATE_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int SLOT_COUNT = 2;
    private static final int[] SLOTS = {TEMPLATE_SLOT, OUTPUT_SLOT};

    private NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    private LazyOptional<IItemHandler> automation = LazyOptional.of(() -> new SidedInvWrapper(this, Direction.UP));

    public CreativeChestBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.CREATIVE_CHEST.get(), pos, state);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, stacks);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, stacks);
        }
    }

    public void refillOutput() {
        ItemStack template = getItem(TEMPLATE_SLOT);
        ItemStack output = getItem(OUTPUT_SLOT);
        int count = template.isEmpty() ? 0 : Math.min(getMaxStackSize(), template.getMaxStackSize());
        if (count != output.getCount() || !ItemStack.isSameItemSameTags(template, output)) {
            setItem(OUTPUT_SLOT, template.copyWithCount(count));
        }
    }

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.extrautilitiesrebirth.creative_chest");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new CreativeChestGUIMenu(id, inventory, this);
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return stacks;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        stacks = items;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == TEMPLATE_SLOT;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == OUTPUT_SLOT;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (!isRemoved() && capability == ForgeCapabilities.ITEM_HANDLER) {
            return automation.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        automation.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        automation = LazyOptional.of(() -> new SidedInvWrapper(this, Direction.UP));
    }
}
