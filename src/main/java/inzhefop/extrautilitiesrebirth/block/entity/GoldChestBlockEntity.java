package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import inzhefop.extrautilitiesrebirth.world.inventory.GoldenChestGUIMenu;
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

import javax.annotation.Nullable;
import java.util.stream.IntStream;

public class GoldChestBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    private static final int SLOT_COUNT = 54;
    private static final int[] SLOTS = IntStream.range(0, SLOT_COUNT).toArray();

    private NonNullList<ItemStack> stacks = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);

    public GoldChestBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.GOLD_CHEST.get(), pos, state);
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

    @Override
    public int getContainerSize() {
        return SLOT_COUNT;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.extrautilitiesrebirth.gold_chest");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new GoldenChestGUIMenu(id, inventory, this);
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
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }
}
