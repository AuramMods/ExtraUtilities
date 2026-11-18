package inzhefop.extrautilitiesrebirth.world.inventory;

import inzhefop.extrautilitiesrebirth.block.entity.MechanicalUserBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class MechanicalUserGUIMenu extends AbstractContainerMenu {
    private static final int UPGRADE_SLOT = MechanicalUserBlockEntity.UPGRADE_SLOT;
    private static final int PLAYER_START = UPGRADE_SLOT + 1;
    private static final int HOTBAR_START = PLAYER_START + 27;

    public final Level world;
    public final BlockPos position;
    private final MechanicalUserBlockEntity user;

    public MechanicalUserGUIMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        super(ExtrautilitiesrebirthModMenus.MECHANICAL_USER_GUI.get(), id);
        world = inventory.player.level();
        position = extraData == null ? inventory.player.blockPosition() : extraData.readBlockPos();
        user = extraData != null && world.getBlockEntity(position) instanceof MechanicalUserBlockEntity entity ? entity : null;
        IItemHandler slots = !world.isClientSide && user != null
                ? user.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseGet(() -> new ItemStackHandler(PLAYER_START))
                : new ItemStackHandler(PLAYER_START);

        for (int index = 0; index < UPGRADE_SLOT; index++) {
            addMachineSlot(slots, index, 52 + index % 4 * 18, 19 + index / 4 * 18);
        }
        addMachineSlot(slots, UPGRADE_SLOT, 8, 19);

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int index = 0; index < 9; index++) {
            addSlot(new Slot(inventory, index, 8 + index * 18, 142));
        }
    }

    private void addMachineSlot(IItemHandler handler, int index, int x, int y) {
        addSlot(new SlotItemHandler(handler, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return user != null && user.canPlaceItem(index, stack);
            }

            @Override
            public void setChanged() {
                if (!world.isClientSide && user != null) {
                    user.setChanged();
                }
            }
        });
    }

    public String modeLabel() {
        return user == null ? "" : user.mode().label();
    }

    @Override
    public boolean stillValid(Player player) {
        return world.isClientSide || (player.level() == world && user != null && user.stillValid(player));
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId != 0 || world.isClientSide || user == null || !stillValid(player)) {
            return false;
        }
        user.toggleMode();
        return true;
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
            boolean upgrading = getSlot(UPGRADE_SLOT).mayPlace(stack);
            if (!moveItemStackTo(stack, upgrading ? UPGRADE_SLOT : 0, upgrading ? PLAYER_START : UPGRADE_SLOT, false)) {
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
