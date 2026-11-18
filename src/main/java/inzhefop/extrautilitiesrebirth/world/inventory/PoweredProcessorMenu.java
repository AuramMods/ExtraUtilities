package inzhefop.extrautilitiesrebirth.world.inventory;

import inzhefop.extrautilitiesrebirth.block.entity.PoweredProcessorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;
import java.util.function.ToIntFunction;

public abstract class PoweredProcessorMenu extends AbstractContainerMenu {
    private static final int PROGRESS = 0;
    private static final int ENERGY = 1;

    private final Level world;
    private final PoweredProcessorBlockEntity machine;
    private final int playerStart;
    private final int hotbarStart;
    private final int[] display = new int[2];

    protected PoweredProcessorMenu(MenuType<?> type, int id, Inventory inventory, @Nullable BlockPos pos,
                                   Class<? extends PoweredProcessorBlockEntity> expected, int[][] slotPositions) {
        super(type, id);
        world = inventory.player.level();
        var candidate = pos == null ? null : world.getBlockEntity(pos);
        machine = expected.isInstance(candidate) ? expected.cast(candidate) : null;
        playerStart = slotPositions.length;
        hotbarStart = playerStart + 27;
        IItemHandler internal = !world.isClientSide && machine != null
                ? machine.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseGet(() -> new ItemStackHandler(playerStart))
                : new ItemStackHandler(playerStart);

        for (int index = 0; index < playerStart; index++) {
            int slot = index;
            addSlot(new SlotItemHandler(internal, slot, slotPositions[slot][0], slotPositions[slot][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return machine != null && machine.canPlaceItem(slot, stack);
                }

                @Override
                public void setChanged() {
                    if (!world.isClientSide && machine != null) {
                        machine.setChanged();
                    }
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int index = 0; index < 9; index++) {
            addSlot(new Slot(inventory, index, 8 + index * 18, 142));
        }

        track(PROGRESS, PoweredProcessorBlockEntity::progressPixels);
        track(ENERGY, PoweredProcessorBlockEntity::energyPixels);
    }

    @Override
    public boolean stillValid(Player player) {
        return world.isClientSide || (player.level() == world && machine != null && machine.stillValid(player));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < playerStart) {
            if (!moveItemStackTo(stack, playerStart, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveToMachine(stack)) {
            if (index < hotbarStart) {
                if (!moveItemStackTo(stack, hotbarStart, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, playerStart, hotbarStart, false)) {
                return ItemStack.EMPTY;
            }
        }

        slot.set(stack.isEmpty() ? ItemStack.EMPTY : stack);
        slot.onTake(player, stack);
        return original;
    }

    private boolean moveToMachine(ItemStack stack) {
        int upgradeSlot = playerStart - 1;
        if (getSlot(upgradeSlot).mayPlace(stack)) {
            return moveItemStackTo(stack, upgradeSlot, playerStart, false);
        }
        for (int slot = 0; slot < upgradeSlot - 1; slot++) {
            if (getSlot(slot).mayPlace(stack) && moveItemStackTo(stack, slot, slot + 1, false)) {
                return true;
            }
        }
        return false;
    }

    private void track(int slot, ToIntFunction<PoweredProcessorBlockEntity> read) {
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return world.isClientSide || machine == null ? display[slot] : read.applyAsInt(machine);
            }

            @Override
            public void set(int value) {
                display[slot] = value;
            }
        });
    }

    public int progressPixels() {
        return display[PROGRESS];
    }

    public int energyPixels() {
        return display[ENERGY];
    }
}
