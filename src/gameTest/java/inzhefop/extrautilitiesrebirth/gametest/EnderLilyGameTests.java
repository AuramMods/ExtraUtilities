package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder("extrautilitiesrebirth")
@PrefixGameTestTemplate(false)
public class EnderLilyGameTests {
    @GameTest(template = "machine_test")
    public static void seedsPlantAndGrowOnEndStoneAndEnderCore(GameTestHelper test) {
        var level = test.getLevel();
        var stages = stages();
        var player = test.makeMockPlayer();
        var pos = new BlockPos(3, 2, 3);
        var actual = test.absolutePos(pos);
        var support = test.absolutePos(pos.below());
        var hit = new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false);
        var core = ExtrautilitiesrebirthModBlocks.ENDER_CORE.get();
        for (Block ground : List.of(Blocks.DIRT, Blocks.END_STONE, core)) {
            test.setBlock(pos.below(), ground);
            test.setBlock(pos, Blocks.AIR);
            var seeds = new ItemStack(stages.get(0), 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, seeds);
            var placement = seeds.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
            if (ground == Blocks.DIRT) {
                test.assertTrue(!placement.consumesAction() && seeds.getCount() == 2, "Dirt should reject the seed without consuming it");
                test.assertBlockPresent(Blocks.AIR, pos);
                continue;
            }
            test.assertTrue(placement.consumesAction() && seeds.getCount() == 1, "Valid placement should consume exactly one seed");
            for (int stage = 0; stage < stages.size() - 1; stage += ground == core ? 2 : 1) {
                test.assertBlockPresent(stages.get(stage), pos);
                test.assertTrue(level.getBlockTicks().hasScheduledTick(actual, stages.get(stage)), "Each immature stage should schedule growth");
                level.getBlockState(actual).tick(level, actual, level.random);
            }
            test.assertBlockPresent(stages.get(7), pos);
        }
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void ripeEnderLiliesRegrowOrDropBothPartsWhenBroken(GameTestHelper test) {
        var level = test.getLevel();
        var stages = stages();
        var pos = new BlockPos(3, 2, 3);
        var actual = test.absolutePos(pos);
        var player = test.makeMockPlayer();
        var hit = new BlockHitResult(Vec3.atCenterOf(actual), Direction.UP, actual, false);
        test.setBlock(pos.below(), Blocks.END_STONE);
        test.setBlock(pos, stages.get(7));
        test.assertTrue(level.getBlockState(actual).use(level, player, InteractionHand.MAIN_HAND, hit).consumesAction(),
                "Right-click should harvest the ripe plant");
        test.assertBlockPresent(stages.get(0), pos);
        var drops = test.getEntities(EntityType.ITEM, pos, 2);
        test.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(Items.ENDER_PEARL) && drops.get(0).getItem().getCount() == 1,
                "Picking the fruit should drop one pearl and keep the seed planted");
        drops.get(0).discard();

        for (Block stage : stages) {
            test.setBlock(pos, stage);
            test.assertTrue(level.destroyBlock(actual, true), "Automation should be able to break the plant");
            drops = test.getEntities(EntityType.ITEM, pos, 2);
            var count = stage == stages.get(7) ? 2 : 1;
            test.assertTrue(drops.size() == count && drops.stream().anyMatch(item -> item.getItem().is(stages.get(0).asItem())
                    && item.getItem().getCount() == 1), "Breaking a plant should drop one seed");
            test.assertTrue(count == 1 || drops.stream().anyMatch(item -> item.getItem().is(Items.ENDER_PEARL) && item.getItem().getCount() == 1),
                    "Breaking a ripe plant should also drop one pearl");
            drops.forEach(item -> item.discard());
        }
        test.setBlock(pos, stages.get(7));
        test.setBlock(pos.below(), Blocks.AIR);
        test.assertBlockPresent(Blocks.AIR, pos);
        test.assertTrue(test.getEntities(EntityType.ITEM, pos, 2).size() == 2, "A ripe plant should drop the seed and fruit when its support is broken");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void onlyRipeEnderLiliesDealContactDamageAndEndstoneDropsASeed(GameTestHelper test) {
        var level = test.getLevel();
        var stages = stages();
        var pos = new BlockPos(3, 2, 3);
        var actual = test.absolutePos(pos);
        var pig = test.spawn(EntityType.PIG, pos);
        var health = pig.getHealth();
        test.setBlock(pos.below(), Blocks.END_STONE);
        for (Block stage : stages) {
            test.setBlock(pos, stage);
            var state = level.getBlockState(actual);
            test.assertTrue(state.getCollisionShape(level, actual).isEmpty(), "Plants should allow passage at every stage");
            state.entityInside(level, actual, pig);
            test.assertTrue(pig.getHealth() == (stage == stages.get(7) ? health - 1 : health), "Only the ripe plant should hurt on contact");
        }
        pig.discard();
        test.setBlock(pos, ExtrautilitiesrebirthModBlocks.ENDER_LILLY_ENDSTONE.get());
        level.destroyBlock(actual, true);
        var drops = test.getEntities(EntityType.ITEM, pos, 2);
        test.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(stages.get(0).asItem()) && drops.get(0).getItem().getCount() == 1,
                "Ender Lily Endstone should be a natural source of seeds");
        test.succeed();
    }

    private static List<Block> stages() {
        return List.of(ExtrautilitiesrebirthModBlocks.ENDER_LILLY_STAGE_1.get(), ExtrautilitiesrebirthModBlocks.ENDER_LILLY_STAGE_2.get(),
                ExtrautilitiesrebirthModBlocks.ENDER_LILLY_STAGE_3.get(), ExtrautilitiesrebirthModBlocks.ENDER_LILLY_STAGE_4.get(),
                ExtrautilitiesrebirthModBlocks.ENDER_LILLY_STAGE_5.get(), ExtrautilitiesrebirthModBlocks.ENDER_LILLY_STAGE_6.get(),
                ExtrautilitiesrebirthModBlocks.ENDER_LILLY_STAGE_7.get(), ExtrautilitiesrebirthModBlocks.ENDER_LILLY_STAGE_8.get());
    }
}
