package inzhefop.extrautilitiesrebirth.world.inventory;

import inzhefop.extrautilitiesrebirth.block.entity.CreativeChestBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CreativeChestGUIMenu extends AbstractContainerMenu {
    private static final int PLAYER_START = CreativeChestBlockEntity.SLOT_COUNT;
    private static final int HOTBAR_START = PLAYER_START + 27;

    private final Container chest;

    public CreativeChestGUIMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, new SimpleContainer(PLAYER_START));
    }

    public CreativeChestGUIMenu(int id, Inventory inventory, Container chest) {
        super(ExtrautilitiesrebirthModMenus.CREATIVE_CHEST_GUI.get(), id);
        this.chest = chest;
        addSlot(new Slot(chest, CreativeChestBlockEntity.TEMPLATE_SLOT, 52, 35));
        addSlot(new Slot(chest, CreativeChestBlockEntity.OUTPUT_SLOT, 106, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
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
        return chest.stillValid(player);
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
        } else if (!moveItemStackTo(stack, CreativeChestBlockEntity.TEMPLATE_SLOT, CreativeChestBlockEntity.OUTPUT_SLOT, false)) {
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
