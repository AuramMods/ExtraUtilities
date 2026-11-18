package inzhefop.extrautilitiesrebirth.world.inventory;

import inzhefop.extrautilitiesrebirth.block.entity.SortingMachineBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class SorterGUIMenu extends AbstractContainerMenu {
    private static final int UPGRADE_SLOT = SortingMachineBlockEntity.UPGRADE_SLOT;
    private static final int FILTER_SLOT = SortingMachineBlockEntity.FILTER_SLOT;
    private static final int PLAYER_START = FILTER_SLOT + 1;
    private static final int HOTBAR_START = PLAYER_START + 27;
    private static final int VALID_FILTER = 1;
    private static final int MATCHING_OUTPUT = 2;
    private static final int OTHER_OUTPUT = 4;

    private final Level world;
    private final SortingMachineBlockEntity sorter;
    private int status;

    public SorterGUIMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        super(ExtrautilitiesrebirthModMenus.SORTER_GUI.get(), id);
        world = inventory.player.level();
        BlockPos position = extraData == null ? inventory.player.blockPosition() : extraData.readBlockPos();
        sorter = extraData != null && world.getBlockEntity(position) instanceof SortingMachineBlockEntity machine ? machine : null;
        IItemHandler slots = !world.isClientSide && sorter != null
                ? sorter.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseGet(() -> new ItemStackHandler(PLAYER_START))
                : new ItemStackHandler(PLAYER_START);

        for (int index = 0; index < UPGRADE_SLOT; index++) {
            addMachineSlot(slots, index, 44 + index % 5 * 18, 23 + index / 5 * 18);
        }
        addMachineSlot(slots, UPGRADE_SLOT, 8, 23);
        addMachineSlot(slots, FILTER_SLOT, 8, 42);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int index = 0; index < 9; index++) {
            addSlot(new Slot(inventory, index, 8 + index * 18, 142));
        }

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (world.isClientSide || sorter == null || sorter.isRemoved()) {
                    return status;
                }
                int value = sorter.filter() == null ? 0 : VALID_FILTER;
                if (sorter.output(true).isPresent()) {
                    value |= MATCHING_OUTPUT;
                }
                if (sorter.output(false).isPresent()) {
                    value |= OTHER_OUTPUT;
                }
                return value;
            }

            @Override
            public void set(int value) {
                status = value;
            }
        });
    }

    private void addMachineSlot(IItemHandler handler, int index, int x, int y) {
        addSlot(new SlotItemHandler(handler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return sorter != null && sorter.canPlaceItem(index, stack);
            }

            @Override
            public void setChanged() {
                if (!world.isClientSide && sorter != null) {
                    sorter.setChanged();
                }
            }
        });
    }

    public String filterLabel() {
        ItemStack filter = getSlot(FILTER_SLOT).getItem();
        return filter.hasCustomHoverName() ? filter.getHoverName().getString() : "";
    }

    public boolean hasValidFilter() {
        return (status & VALID_FILTER) != 0;
    }

    public boolean hasMatchingOutput() {
        return (status & MATCHING_OUTPUT) != 0;
    }

    public boolean hasOtherOutput() {
        return (status & OTHER_OUTPUT) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return world.isClientSide || (player.level() == world && sorter != null && sorter.stillValid(player));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < PLAYER_START) {
            if (!moveItemStackTo(stack, PLAYER_START, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int target = getSlot(UPGRADE_SLOT).mayPlace(stack) ? UPGRADE_SLOT
                    : getSlot(FILTER_SLOT).mayPlace(stack) ? FILTER_SLOT : 0;
            int end = target == 0 ? UPGRADE_SLOT : target + 1;
            if (!moveItemStackTo(stack, target, end, false)) {
                if (index < HOTBAR_START) {
                    if (!moveItemStackTo(stack, HOTBAR_START, slots.size(), true)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
        slot.onTake(player, stack);
        return original;
    }
}
