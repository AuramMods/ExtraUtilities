package inzhefop.extrautilitiesrebirth.procedures;

import inzhefop.extrautilitiesrebirth.block.MechanicalUserBlock;
import inzhefop.extrautilitiesrebirth.block.entity.MechanicalUserBlockEntity;
import inzhefop.extrautilitiesrebirth.block.entity.MechanicalUserBlockEntity.Mode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;

import java.util.Comparator;
import java.util.List;

public class MechanicalUserUpdateTickProcedure {
    public static void execute(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(level.getBlockEntity(pos) instanceof MechanicalUserBlockEntity user)) {
            return;
        }
        Direction facing = state.getValue(MechanicalUserBlock.FACING);
        BlockPos target = pos.relative(facing);
        Mode mode = user.mode();
        int input = findInput(user, mode);
        if (input < 0 || (mode != Mode.ATTACK && !level.getBlockState(target).isAir())) {
            MechanicalWork.reset(user);
            return;
        }

        List<Mob> targets = mode == Mode.ATTACK
                ? level.getEntitiesOfClass(Mob.class, new AABB(target).inflate(2), mob -> mob.isAlive() && mob.isAttackable())
                : List.of();
        if (mode == Mode.ATTACK && targets.isEmpty()) {
            MechanicalWork.reset(user);
            return;
        }
        if (!MechanicalWork.advance(user, user.workSpeed())) {
            return;
        }

        switch (mode) {
            case PLACE_BLOCK -> placeBlock(level, user, input, facing);
            case DROP_ITEM -> dropItem(level, user, input, target);
            case ATTACK -> attack(level, user, input, target, targets);
        }
    }

    private static int findInput(MechanicalUserBlockEntity user, Mode mode) {
        for (int slot = 0; slot < MechanicalUserBlockEntity.UPGRADE_SLOT; slot++) {
            ItemStack stack = user.getItem(slot);
            if (!stack.isEmpty() && switch (mode) {
                case PLACE_BLOCK -> stack.getItem() instanceof BlockItem;
                case DROP_ITEM -> true;
                case ATTACK -> stack.getItem() instanceof SwordItem;
            }) {
                return slot;
            }
        }
        return -1;
    }

    private static void placeBlock(ServerLevel level, MechanicalUserBlockEntity user, int slot, Direction facing) {
        var actor = FakePlayerFactory.getMinecraft(level);
        var previousItem = actor.getMainHandItem();
        Vec3 previousPosition = actor.position();
        float previousPitch = actor.getXRot();
        float previousYaw = actor.getYRot();
        BlockPos pos = user.getBlockPos();
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 face = center.add(facing.getStepX() * 0.5, facing.getStepY() * 0.5, facing.getStepZ() * 0.5);
        var hit = new BlockHitResult(face, facing, pos, false);

        try {
            actor.setPos(center.x, center.y - actor.getEyeHeight(), center.z);
            actor.setYRot(facing.getAxis().isHorizontal() ? facing.toYRot() : 0);
            actor.setXRot(facing.getStepY() * -90);
            ItemStack input = user.getItem(slot).copy();
            actor.setItemInHand(InteractionHand.MAIN_HAND, input);
            if (input.useOn(new UseOnContext(actor, InteractionHand.MAIN_HAND, hit)).consumesAction()) {
                user.setItem(slot, actor.getMainHandItem());
            }
        } finally {
            actor.setItemInHand(InteractionHand.MAIN_HAND, previousItem);
            actor.setPos(previousPosition.x, previousPosition.y, previousPosition.z);
            actor.setXRot(previousPitch);
            actor.setYRot(previousYaw);
        }
    }

    private static void dropItem(ServerLevel level, MechanicalUserBlockEntity user, int slot, BlockPos target) {
        Vec3 center = Vec3.atCenterOf(target);
        var item = new ItemEntity(level, center.x, center.y, center.z, user.getItem(slot).copyWithCount(1));
        item.setPickUpDelay(10);
        if (level.addFreshEntity(item)) {
            user.removeItem(slot, 1);
        }
    }

    private static void attack(ServerLevel level, MechanicalUserBlockEntity user, int slot, BlockPos target, List<Mob> mobs) {
        Vec3 center = Vec3.atCenterOf(target);
        mobs.sort(Comparator.comparingDouble(mob -> mob.distanceToSqr(center)));
        ItemStack sword = user.getItem(slot).copy();
        for (Mob mob : mobs) {
            if (mob.hurt(level.damageSources().generic(), 4) && sword.hurt(1, level.getRandom(), null)) {
                sword.shrink(1);
                sword.setDamageValue(0);
                if (sword.isEmpty()) {
                    break;
                }
            }
        }
        user.setItem(slot, sword);
    }
}
