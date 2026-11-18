package inzhefop.extrautilitiesrebirth.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

final class ToolHarvest {
    private ToolHarvest() {
    }

    static boolean breakBlock(ServerLevel level, BlockPos pos, BlockState state, Player player, Direction face, ItemStack tool) {
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, tool)) {
            return false;
        }
        var event = new BlockEvent.BreakEvent(level, pos, state, player);
        return !MinecraftForge.EVENT_BUS.post(event) && level.getBlockState(pos) == state && level.destroyBlock(pos, false, player);
    }
}
