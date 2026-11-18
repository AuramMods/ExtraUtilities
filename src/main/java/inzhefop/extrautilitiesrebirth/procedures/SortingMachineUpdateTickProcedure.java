package inzhefop.extrautilitiesrebirth.procedures;

import inzhefop.extrautilitiesrebirth.block.entity.SortingMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

public class SortingMachineUpdateTickProcedure {
    public static void execute(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof SortingMachineBlockEntity sorter)) {
            return;
        }
        var data = sorter.getPersistentData();
        int ticks = data.getInt("ticks") + 1;
        if (ticks < sorter.ticksPerTransfer()) {
            data.putDouble("ticks", ticks);
            return;
        }
        data.putDouble("ticks", 0);

        var filter = sorter.filter();
        if (filter == null) {
            return;
        }
        IItemHandler matching = sorter.output(true).orElse(null);
        IItemHandler other = sorter.output(false).orElse(null);
        if (matching == null || other == null) {
            return;
        }

        for (int slot = 0; slot < SortingMachineBlockEntity.UPGRADE_SLOT; slot++) {
            ItemStack stack = sorter.getItem(slot);
            if (!stack.isEmpty()) {
                IItemHandler destination = stack.is(filter) ? matching : other;
                if (ItemHandlerHelper.insertItemStacked(destination, stack.copyWithCount(1), false).isEmpty()) {
                    sorter.removeItem(slot, 1);
                }
            }
        }
    }
}
