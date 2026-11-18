package inzhefop.extrautilitiesrebirth.procedures;

import inzhefop.extrautilitiesrebirth.block.MechanicalMinerBlock;
import inzhefop.extrautilitiesrebirth.block.entity.MechanicalMinerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;

import java.util.List;

public class MechanicalMinerUpdateTickProcedure {
    private static final TagKey<Item> UNGROWN_LILIES =
            ItemTags.create(new ResourceLocation("forge", "ungrown_ender_lilly_states"));

    public static void execute(ServerLevel level, BlockPos pos, BlockState state) {
        if (!(level.getBlockEntity(pos) instanceof MechanicalMinerBlockEntity miner) || !miner.flushPendingDrops()) {
            return;
        }

        BlockPos target = pos.relative(state.getValue(MechanicalMinerBlock.FACING));
        BlockState targetState = level.getBlockState(target);
        if (targetState.isAir() || targetState.is(Blocks.END_PORTAL_FRAME) || targetState.getDestroySpeed(level, target) < 0
                || targetState.getBlock() instanceof LiquidBlock || targetState.getBlock().asItem().getDefaultInstance().is(UNGROWN_LILIES)) {
            MechanicalWork.reset(miner);
            return;
        }
        if (!miner.hasOutputSpace()) {
            return;
        }

        if (MechanicalWork.advance(miner, miner.miningSpeed())) {
            harvest(level, miner, target, targetState);
        }
    }

    private static void harvest(ServerLevel level, MechanicalMinerBlockEntity miner, BlockPos target, BlockState state) {
        var actor = FakePlayerFactory.getMinecraft(level);
        var previousItem = actor.getMainHandItem();
        var tool = new ItemStack(Items.DIAMOND_PICKAXE);
        actor.setItemInHand(InteractionHand.MAIN_HAND, tool);
        try {
            if (!actor.hasCorrectToolForDrops(state)) {
                return;
            }
            var event = new BlockEvent.BreakEvent(level, target, state, actor);
            if (MinecraftForge.EVENT_BUS.post(event) || level.getBlockState(target) != state) {
                return;
            }

            List<ItemStack> drops = Block.getDrops(state, level, target, level.getBlockEntity(target), actor, tool);
            if (!level.destroyBlock(target, false, actor)) {
                return;
            }

            state.spawnAfterBreak(level, target, tool, false);
            if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
                miner.addMinedDrops(drops);
                miner.flushPendingDrops();
                ExperienceOrb.award(level, Vec3.atCenterOf(target), event.getExpToDrop());
            }
        } finally {
            actor.setItemInHand(InteractionHand.MAIN_HAND, previousItem);
        }
    }

}
