package inzhefop.extrautilitiesrebirth.world.inventory;

import inzhefop.extrautilitiesrebirth.block.entity.MechanicalMinerBlockEntity;
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

public class MechanicalMinerGUIMenu extends AbstractContainerMenu {
    private static final int UPGRADE_SLOT = MechanicalMinerBlockEntity.UPGRADE_SLOT;
    private static final int PLAYER_START = UPGRADE_SLOT + 1;
    private static final int HOTBAR_START = PLAYER_START + 27;

    public final Level world;
    public final BlockPos position;
    private final MechanicalMinerBlockEntity miner;

    public MechanicalMinerGUIMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        super(ExtrautilitiesrebirthModMenus.MECHANICAL_MINER_GUI.get(), id);
        world = inventory.player.level();
        position = extraData == null ? inventory.player.blockPosition() : extraData.readBlockPos();
        miner = extraData != null && world.getBlockEntity(position) instanceof MechanicalMinerBlockEntity entity ? entity : null;
        IItemHandler slots = !world.isClientSide && miner != null
                ? miner.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseGet(() -> new ItemStackHandler(PLAYER_START))
                : new ItemStackHandler(PLAYER_START);

        for (int index = 0; index < UPGRADE_SLOT; index++) {
            addSlot(new SlotItemHandler(slots, index, 61 + index % 3 * 18, 20 + index / 3 * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addSlot(new SlotItemHandler(slots, UPGRADE_SLOT, 7, 20) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return miner != null && miner.canPlaceItem(UPGRADE_SLOT, stack);
            }

            @Override
            public void setChanged() {
                if (!world.isClientSide && miner != null) {
                    miner.setChanged();
                }
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int index = 0; index < 9; index++) {
            addSlot(new Slot(inventory, index, 8 + index * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return world.isClientSide || (player.level() == world && miner != null && miner.stillValid(player));
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
        } else if (!getSlot(UPGRADE_SLOT).mayPlace(stack) || !moveItemStackTo(stack, UPGRADE_SLOT, PLAYER_START, false)) {
            if (index < HOTBAR_START) {
                if (!moveItemStackTo(stack, HOTBAR_START, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, PLAYER_START, HOTBAR_START, false)) {
                return ItemStack.EMPTY;
            }
        }
        slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
        slot.onTake(player, stack);
        return original;
    }
}
