package inzhefop.extrautilitiesrebirth.world.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

abstract class StorageMenu extends AbstractContainerMenu {
    static final int STORAGE_SLOTS = 54;
    private static final int HOTBAR_START = STORAGE_SLOTS + 27;

    protected StorageMenu(MenuType<?> type, int id) {
        super(type, id);
    }

    protected final void addPlayerSlots(Inventory inventory, int lockedSlot) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 134 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            int x = 8 + column * 18;
            if (column == lockedSlot) {
                addSlot(new Slot(inventory, column, x, 192) {
                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            } else {
                addSlot(new Slot(inventory, column, x, 192));
            }
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < STORAGE_SLOTS) {
            if (!moveItemStackTo(stack, STORAGE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, STORAGE_SLOTS, false)) {
            boolean moved = index < HOTBAR_START
                    ? moveItemStackTo(stack, HOTBAR_START, slots.size(), true)
                    : moveItemStackTo(stack, STORAGE_SLOTS, HOTBAR_START, false);
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }
}
