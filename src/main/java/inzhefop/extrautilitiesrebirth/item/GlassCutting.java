package inzhefop.extrautilitiesrebirth.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;

final class GlassCutting {
    private GlassCutting() {
    }

    static InteractionResult use(UseOnContext context, int cooldown) {
        var player = context.getPlayer();
        var stack = context.getItemInHand();
        var level = context.getLevel();
        var clicked = context.getClickedPos();
        var state = level.getBlockState(clicked);
        var item = stack.getItem();
        if (player == null || player.getCooldowns().isOnCooldown(item)
                || !(state.is(Tags.Blocks.GLASS) || state.is(Tags.Blocks.GLASS_PANES))) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            if (!ToolHarvest.breakBlock(serverLevel, clicked, state, player, context.getClickedFace(), stack)) {
                return InteractionResult.FAIL;
            }
            if (!player.getAbilities().instabuild) {
                Block.popResource(level, clicked, new ItemStack(state.getBlock()));
                stack.hurtAndBreak(1, player, holder -> holder.broadcastBreakEvent(context.getHand()));
            }
            if (cooldown > 0) {
                player.getCooldowns().addCooldown(item, cooldown);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
