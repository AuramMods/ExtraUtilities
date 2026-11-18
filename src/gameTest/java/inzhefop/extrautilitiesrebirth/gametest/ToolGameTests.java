package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(ExtrautilitiesrebirthMod.MODID)
@PrefixGameTestTemplate(false)
public class ToolGameTests {
    @GameTest(template = "machine_test")
    public static void bothCuttersRecoverPanesAndModGlassThroughRightClick(GameTestHelper test) {
        var level = test.getLevel();
        var player = test.makeMockSurvivalPlayer();
        var pos = new BlockPos(3, 2, 3);
        var actual = test.absolutePos(pos);
        var reinforced = ExtrautilitiesrebirthModItems.REINFORCED_GLASS_CUTTER.get();
        var cutter = ExtrautilitiesrebirthModItems.GLASS_CUTTER.get();
        for (var item : List.of(cutter, reinforced)) {
            var tool = new ItemStack(item);
            player.setItemInHand(InteractionHand.OFF_HAND, tool);
            int used = 0;
            for (var glass : List.of(Blocks.BLUE_STAINED_GLASS_PANE, ExtrautilitiesrebirthModBlocks.DARK_ETHEREAL_GLASS.get())) {
                test.setBlock(pos, glass);
                player.getCooldowns().removeCooldown(item);
                test.assertTrue(rightClick(player, InteractionHand.OFF_HAND, actual).consumesAction(), "The cutter should accept a glass block or pane");
                test.assertBlockPresent(Blocks.AIR, pos);
                var drops = test.getEntities(EntityType.ITEM, pos, 2);
                test.assertTrue(drops.size() == 1 && drops.get(0).getItem().is(glass.asItem()) && drops.get(0).getItem().getCount() == 1,
                        "The recovered item should be the exact glass type");
                drops.forEach(Entity::discard);
                test.assertTrue(tool.getDamageValue() == (item == cutter ? ++used : 0), "Only the ordinary cutter should take wear");
            }
            test.setBlock(pos, Blocks.STONE);
            test.assertTrue(!rightClick(player, InteractionHand.OFF_HAND, actual).consumesAction(), "Stone should ignore glass cutters");
            test.assertBlockPresent(Blocks.STONE, pos);
        }
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void glassCutterCraftingRemainderPreservesNameAndWear(GameTestHelper test) {
        var tool = new ItemStack(ExtrautilitiesrebirthModItems.GLASS_CUTTER.get()).setHoverName(Component.literal("Shard Cutter"));
        tool.setDamageValue(37);
        var remainder = tool.getCraftingRemainingItem();
        test.assertTrue(remainder.is(tool.getItem()) && remainder.getDamageValue() == 38 && remainder.getHoverName().equals(tool.getHoverName()),
                "Crafting should return the same named cutter with one point of wear");
        tool.setDamageValue(tool.getMaxDamage() - 1);
        test.assertTrue(tool.getCraftingRemainingItem().isEmpty(), "A worn-out cutter should break after its final use");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void fireAxeFellsConnectedLogsAfterTheOriginalBreak(GameTestHelper test) {
        var level = test.getLevel();
        var player = test.makeMockSurvivalPlayer();
        var pos = new BlockPos(2, 2, 2);
        var actual = test.absolutePos(pos);
        var logs = List.of(pos.above(), pos.above(2).east(), pos.above(3).east());
        for (var log : logs) {
            test.setBlock(log, Blocks.OAK_LOG);
        }
        test.setBlock(pos.north(2), Blocks.BIRCH_LOG);
        test.setBlock(pos.east(), Blocks.COBBLESTONE);
        var axe = new ItemStack(ExtrautilitiesrebirthModItems.FIRE_AXE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        axe.mineBlock(level, Blocks.OAK_LOG.defaultBlockState(), actual, player);
        for (var log : logs) {
            test.assertBlockPresent(Blocks.AIR, log);
        }
        test.assertBlockPresent(Blocks.BIRCH_LOG, pos.north(2));
        test.assertBlockPresent(Blocks.COBBLESTONE, pos.east());
        var count = test.getEntities(EntityType.ITEM, pos.above(), 5).stream()
                .filter(entity -> entity.getItem().is(Items.OAK_LOG)).mapToInt(entity -> entity.getItem().getCount()).sum();
        test.assertTrue(count == logs.size(), "Every removed log should drop exactly once");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void theReversingHoeRevertsCropsDirtAndCobblestone(GameTestHelper test) {
        var level = test.getLevel();
        var player = test.makeMockSurvivalPlayer();
        var pos = new BlockPos(3, 2, 3);
        var actual = test.absolutePos(pos);
        var hoe = new ItemStack(ExtrautilitiesrebirthModItems.REVERSING_HOE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, hoe);
        test.setBlock(pos.below(), Blocks.FARMLAND);
        var wheat = (CropBlock) Blocks.WHEAT;
        test.setBlock(pos, wheat.getStateForAge(5));
        test.assertTrue(rightClick(player, InteractionHand.MAIN_HAND, actual).consumesAction()
                && wheat.getAge(level.getBlockState(actual)) == 4, "Using the hoe should regress wheat by one age");
        test.setBlock(pos, Blocks.DIRT);
        test.assertTrue(rightClick(player, InteractionHand.MAIN_HAND, actual).consumesAction(), "The hoe should restore grass");
        test.assertBlockPresent(Blocks.GRASS_BLOCK, pos);
        test.setBlock(pos, Blocks.COBBLESTONE);
        test.assertTrue(rightClick(player, InteractionHand.MAIN_HAND, actual).consumesAction(), "The hoe should restore stone");
        test.assertBlockPresent(Blocks.STONE, pos);
        test.assertTrue(hoe.getDamageValue() == 3, "Only successful reversals should cost durability");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void precisionShearsPickUpAllowableBlocksOnlyIfInventoryHasRoom(GameTestHelper test) {
        var player = test.makeMockSurvivalPlayer();
        var pos = new BlockPos(3, 2, 3);
        var actual = test.absolutePos(pos);
        var shears = new ItemStack(ExtrautilitiesrebirthModItems.PRECISION_SHEARS.get());
        player.setItemInHand(InteractionHand.OFF_HAND, shears);
        player.setShiftKeyDown(true);
        test.setBlock(pos, Blocks.DIAMOND_ORE);
        test.assertTrue(!rightClick(player, InteractionHand.OFF_HAND, actual).consumesAction(), "A stone-tier tool cannot pick up diamond ore");
        test.assertBlockPresent(Blocks.DIAMOND_ORE, pos);
        test.setBlock(pos, Blocks.STONE);
        test.assertTrue(rightClick(player, InteractionHand.OFF_HAND, actual).consumesAction(), "Sneaking should pick up stone into the inventory");
        test.assertBlockPresent(Blocks.AIR, pos);
        test.assertTrue(player.getInventory().countItem(Items.STONE) == 1 && test.getEntities(EntityType.ITEM, pos, 2).isEmpty(),
                "The exact block should appear once in the inventory");
        player.getInventory().items.replaceAll(stack -> new ItemStack(Items.COBBLESTONE, 64));
        test.setBlock(pos, Blocks.STONE);
        test.assertTrue(!rightClick(player, InteractionHand.OFF_HAND, actual).consumesAction(), "A full inventory should reject the pickup");
        test.assertBlockPresent(Blocks.STONE, pos);
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void erosionAndDestructionToolsClearTheirTargetMaterialsAndProtectValuables(GameTestHelper test) {
        var level = test.getLevel();
        var player = test.makeMockSurvivalPlayer();
        var pos = new BlockPos(3, 2, 3);
        var actual = test.absolutePos(pos);
        var pick = new ItemStack(ExtrautilitiesrebirthModItems.DESTRUCTION_PICKAXE.get());
        var stone = Blocks.STONE.defaultBlockState();
        var diamond = Blocks.DIAMOND_ORE.defaultBlockState();
        test.assertTrue(pick.getDestroySpeed(stone) > 35 && pick.getDestroySpeed(diamond) < 1,
                "Destruction Pickaxe speed should target ordinary stone");
        test.assertTrue(Block.getDrops(stone, level, actual, null, player, pick).isEmpty()
                && Block.getDrops(diamond, level, actual, null, player, pick).stream().anyMatch(drop -> drop.is(Items.DIAMOND)),
                "The pickaxe should void stone while valuable ore still yields its loot");
        var shovel = new ItemStack(ExtrautilitiesrebirthModItems.EROSION_SHOVEL.get());
        var dirt = Blocks.DIRT.defaultBlockState();
        test.assertTrue(Block.getDrops(dirt, level, actual, null, player, shovel).isEmpty(), "The erosion shovel should void the primary dirt drop");
        test.setBlock(pos.above(), Blocks.SAND);
        test.setBlock(pos.above(2), Blocks.GRAVEL);
        test.setBlock(pos.above(3), Blocks.DIAMOND_ORE);
        player.setItemInHand(InteractionHand.MAIN_HAND, shovel);
        shovel.mineBlock(level, dirt, actual, player);
        test.assertBlockPresent(Blocks.AIR, pos.above());
        test.assertBlockPresent(Blocks.AIR, pos.above(2));
        test.assertBlockPresent(Blocks.DIAMOND_ORE, pos.above(3));
        test.assertTrue(test.getEntities(EntityType.ITEM, pos.above(), 3).isEmpty() && shovel.getDamageValue() == 3,
                "The falling column should produce no items and count each removed block for wear");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void healingAxeTransfersHealthAndReplenishesHunger(GameTestHelper test) {
        var level = test.getLevel();
        var player = test.makeMockSurvivalPlayer();
        var tool = new ItemStack(ExtrautilitiesrebirthModItems.HEALING_AXE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, tool);
        var target = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(3, 2, 3));
        target.setHealth(4);
        float before = player.getHealth();
        player.attack(target);
        test.assertTrue(target.getHealth() > 4 && player.getHealth() < before && tool.getDamageValue() == 1,
                "The axe should pay some of the player's health to heal a living target");
        var undead = test.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(4, 2, 3));
        player.attack(undead);
        test.assertTrue(undead.getHealth() == 12 && tool.getDamageValue() == 2, "The same transfer should harm undead creatures");
        player.getFoodData().setFoodLevel(10);
        player.tickCount = 40;
        tool.inventoryTick(level, player, 0, true);
        test.assertTrue(player.getFoodData().getFoodLevel() == 11, "Equipping the axe should feed the player slowly");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void kikokuAndEthericSwordApplyTheirArmorPiercingInTheOriginalHit(GameTestHelper test) {
        var level = test.getLevel();
        var player = test.makeMockSurvivalPlayer();
        var pos = new BlockPos(3, 2, 3);
        var unarmored = test.spawnWithNoFreeWill(EntityType.PIG, pos);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ExtrautilitiesrebirthModItems.KIKOKU.get()));
        unarmored.hurt(level.damageSources().playerAttack(player), 2);
        test.assertTrue(unarmored.getHealth() == 4, "A Kikoku hit should include four points of armor-piercing damage exactly once");

        var normalTarget = test.spawnWithNoFreeWill(EntityType.ZOMBIE, pos.east());
        var ethericTarget = test.spawnWithNoFreeWill(EntityType.ZOMBIE, pos.south());
        for (var target : List.of(normalTarget, ethericTarget)) {
            target.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        normalTarget.hurt(level.damageSources().playerAttack(player), 6);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ExtrautilitiesrebirthModItems.ETHERIC_SWORD.get()));
        ethericTarget.hurt(level.damageSources().playerAttack(player), 6);
        test.assertTrue(ethericTarget.getHealth() < normalTarget.getHealth(), "An Etheric hit should hurt more against the same armor");
        test.succeed();
    }

    private static InteractionResult rightClick(Player player, InteractionHand hand, BlockPos pos) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(0, 0.5, 0), Direction.UP, pos, false);
        var context = new UseOnContext(player, hand, hit);
        var item = player.getItemInHand(hand);
        var first = item.onItemUseFirst(context);
        return first == InteractionResult.PASS ? item.useOn(context) : first;
    }
}
