package inzhefop.extrautilitiesrebirth.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AngelBlockItem extends BlockItem {
    public AngelBlockItem(Block block) {
        super(block, new Properties());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        var eye = player.getEyePosition();
        var reach = eye.add(player.getLookAngle().scale(3));
        var ray = level.clip(new ClipContext(eye, reach, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        var target = ray.getBlockPos();
        if (ray.getType() != HitResult.Type.MISS || !level.isEmptyBlock(target) || !level.mayInteract(player, target)) {
            return InteractionResultHolder.pass(stack);
        }
        var placementHit = new BlockHitResult(reach, ray.getDirection(), target, false);
        var result = stack.useOn(new UseOnContext(player, hand, placementHit));
        return new InteractionResultHolder<>(result, stack);
    }
}
