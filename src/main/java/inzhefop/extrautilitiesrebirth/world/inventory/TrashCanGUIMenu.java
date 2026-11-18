package inzhefop.extrautilitiesrebirth.world.inventory;

import inzhefop.extrautilitiesrebirth.block.entity.TrashCanBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class TrashCanGUIMenu extends AbstractContainerMenu {
    private static final int PLAYER_START = TrashCanBlockEntity.SLOT_COUNT;
    private static final int HOTBAR_START = PLAYER_START + 27;

    private final Container trash;

    public TrashCanGUIMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, new SimpleContainer(PLAYER_START));
    }

    public TrashCanGUIMenu(int id, Inventory inventory, Container trash) {
        super(ExtrautilitiesrebirthModMenus.TRASH_CAN_GUI.get(), id);
        this.trash = trash;
        for (int index = 0; index < PLAYER_START; index++) {
            addSlot(new Slot(trash, index, 62 + index % 3 * 18, 17 + index / 3 * 18) {
                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return trash.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (index < PLAYER_START || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (!moveItemStackTo(stack, 0, PLAYER_START, false)) {
            boolean moved = index < HOTBAR_START
                    ? moveItemStackTo(stack, HOTBAR_START, slots.size(), true)
                    : moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false);
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }
}
