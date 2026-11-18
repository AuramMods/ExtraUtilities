package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(ExtrautilitiesrebirthMod.MODID)
@PrefixGameTestTemplate(false)
public class SpikesAndCursedEarthGameTests {
    @GameTest(template = "machine_test")
    public static void spikeTiersDamageLivingMobsAndLeaveItemsIntact(GameTestHelper test) {
        var blocks = List.of(ExtrautilitiesrebirthModBlocks.WOOD_SPIKE.get(), ExtrautilitiesrebirthModBlocks.STONE_SPIKE.get(),
                ExtrautilitiesrebirthModBlocks.IRON_SPIKE.get(), ExtrautilitiesrebirthModBlocks.GOLD_SPIKE.get(),
                ExtrautilitiesrebirthModBlocks.DIAMOND_SPIKE.get(), ExtrautilitiesrebirthModBlocks.CREATIVE_SPIKES.get());
        var damages = List.of(0.5f, 1.2f, 2f, 5f, 12f, 20f);
        var level = test.getLevel();
        var pos = new BlockPos(3, 2, 3);
        var actual = test.absolutePos(pos);
        var dropped = test.spawnItem(Items.DIAMOND, pos);
        for (int i = 0; i < blocks.size(); i++) {
            test.setBlock(pos, blocks.get(i));
            var mob = test.spawnWithNoFreeWill(EntityType.SKELETON, pos.above());
            var state = level.getBlockState(actual);
            state.getBlock().stepOn(level, actual, state, mob);
            test.assertTrue(Math.abs(mob.getHealth() - (20 - damages.get(i))) < 0.001, "Unexpected spike damage from " + blocks.get(i).getName().getString());
            state.entityInside(level, actual, dropped);
            test.assertTrue(dropped.isAlive(), "Items must remain collectible on every spike tier");
            mob.discard();
        }
        test.setBlock(pos, blocks.get(0));
        var animal = test.spawnWithNoFreeWill(EntityType.PIG, pos.above());
        animal.setHealth(1.25f);
        level.getBlockState(actual).entityInside(level, actual, animal);
        test.assertTrue(animal.isAlive() && animal.getHealth() == 1f, "Wooden spikes should stop at one health point");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void goldAwardsExperienceAndDiamondGetsPlayerKillLoot(GameTestHelper test) {
        var pos = new BlockPos(3, 2, 3);
        var level = test.getLevel();
        var actual = test.absolutePos(pos);
        test.setBlock(pos, ExtrautilitiesrebirthModBlocks.GOLD_SPIKE.get());
        var goldVictim = test.spawnWithNoFreeWill(EntityType.SKELETON, pos.above());
        goldVictim.setHealth(1);
        var goldState = level.getBlockState(actual);
        goldState.getBlock().stepOn(level, actual, goldState, goldVictim);
        var xp = test.getEntities(EntityType.EXPERIENCE_ORB, pos, 2);
        test.assertTrue(!goldVictim.isAlive() && goldVictim.getKillCredit() == null && !xp.isEmpty(), "Gold should award experience without player-kill credit");
        xp.forEach(Entity::discard);
        test.setBlock(pos, ExtrautilitiesrebirthModBlocks.DIAMOND_SPIKE.get());
        var diamondVictim = test.spawnWithNoFreeWill(EntityType.SKELETON, pos.above());
        diamondVictim.setHealth(1);
        var diamondState = level.getBlockState(actual);
        diamondState.getBlock().stepOn(level, actual, diamondState, diamondVictim);
        test.assertTrue(!diamondVictim.isAlive() && diamondVictim.getKillCredit() instanceof FakePlayer,
                "Diamond should enable the player-kill entries of the normal loot table");
        test.assertTrue(test.getEntities(EntityType.EXPERIENCE_ORB, pos, 2).isEmpty(), "Diamond should exclusively add player-kill loot");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void dropOfEvilConvertsGrassUsingTheActualHand(GameTestHelper test) {
        var level = test.getLevel();
        var pos = new BlockPos(3, 2, 3);
        var actual = test.absolutePos(pos);
        var player = test.makeMockSurvivalPlayer();
        var stack = new ItemStack(ExtrautilitiesrebirthModItems.DROPOF_EVIL.get(), 2);
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        var click = new BlockHitResult(Vec3.atCenterOf(actual).add(0, 0.5, 0), Direction.UP, actual, false);
        test.setBlock(pos, Blocks.STONE);
        test.assertTrue(!stack.useOn(new UseOnContext(player, InteractionHand.OFF_HAND, click)).consumesAction() && stack.getCount() == 2,
                "Stone should leave the Drop of Evil intact");
        test.setBlock(pos, Blocks.GRASS_BLOCK);
        test.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.OFF_HAND, click)).consumesAction() && stack.getCount() == 1,
                "The offhand interaction should spend exactly one Drop of Evil");
        test.assertBlockPresent(ExtrautilitiesrebirthModBlocks.CURSED_EARTH.get(), pos);
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void renamedWitherSkeletonsCanDropEvilAndRenamedPigsCannot(GameTestHelper test) {
        var level = test.getLevel();
        var pos = new BlockPos(3, 2, 3);
        var item = ExtrautilitiesrebirthModItems.DROPOF_EVIL.get();
        boolean found = false;
        for (int i = 0; i < 160; i++) {
            var mob = test.spawnWithNoFreeWill(EntityType.WITHER_SKELETON, pos);
            mob.setCustomName(Component.literal("Coal Miner"));
            mob.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            mob.discard();
            var drops = test.getEntities(EntityType.ITEM, pos, 2);
            found |= drops.stream().anyMatch(drop -> drop.getItem().is(item));
            drops.forEach(Entity::discard);
            if (found) {
                break;
            }
        }
        test.assertTrue(found, "Wither Skeleton loot must use the mob's type even if it has a custom name");
        for (int i = 0; i < 40; i++) {
            var mob = test.spawnWithNoFreeWill(EntityType.PIG, pos);
            mob.setCustomName(Component.literal("Wither Skeleton"));
            mob.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
            mob.discard();
        }
        test.assertTrue(test.getEntities(EntityType.ITEM, pos, 2).stream().noneMatch(drop -> drop.getItem().is(item)),
                "Renaming an ordinary animal should leave its drops unchanged");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void cursedEarthSpreadsAndSpawnsWithABoundedLocalPopulation(GameTestHelper test) {
        var level = test.getLevel();
        var pos = new BlockPos(3, 2, 3);
        var actual = test.absolutePos(pos);
        var cursed = ExtrautilitiesrebirthModBlocks.CURSED_EARTH.get();
        for (BlockPos room : BlockPos.betweenClosed(1, 1, 1, 5, 5, 5)) {
            boolean wall = room.getY() == 1 || room.getY() == 5 || room.getX() == 1 || room.getX() == 5 || room.getZ() == 1 || room.getZ() == 5;
            test.setBlock(room, wall ? Blocks.STONE : Blocks.AIR);
        }
        test.setBlock(pos, cursed);
        test.setBlock(pos.east(), Blocks.DIRT);
        var nearby = new AABB(actual).inflate(7, 4, 7);
        test.runAfterDelay(15, () -> {
            test.assertTrue(level.getMaxLocalRawBrightness(actual.above()) < 9, "The enclosed test room must be dark");
            var random = RandomSource.create(6481);
            for (int i = 0; i < 300; i++) {
                level.getBlockState(actual).tick(level, actual, random);
            }
            test.assertBlockPresent(cursed, pos.east());
            var monsters = level.getEntitiesOfClass(Monster.class, nearby);
            test.assertTrue(!monsters.isEmpty() && monsters.size() <= 8, "Dark Cursed Earth should spawn monsters and cap their local population");
            monsters.forEach(Entity::discard);
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void torchlightStopsCoveredCursedEarthFromSpreadingOrSpawning(GameTestHelper test) {
        var level = test.getLevel();
        var pos = new BlockPos(3, 2, 3);
        var actual = test.absolutePos(pos);
        var cursed = ExtrautilitiesrebirthModBlocks.CURSED_EARTH.get();
        test.setBlock(pos.above(3), Blocks.STONE);
        test.setBlock(pos.above().north(), Blocks.SEA_LANTERN);
        test.setBlock(pos.east(), Blocks.GRASS_BLOCK);
        test.setBlock(pos, cursed);
        test.runAfterDelay(15, () -> {
            test.assertTrue(level.getMaxLocalRawBrightness(actual.above()) >= 9, "A light source must illuminate the spawn surface");
            var random = RandomSource.create(2493);
            for (int i = 0; i < 300; i++) {
                level.getBlockState(actual).tick(level, actual, random);
            }
            test.assertBlockPresent(cursed, pos);
            test.assertBlockPresent(Blocks.GRASS_BLOCK, pos.east());
            test.assertTrue(level.getEntitiesOfClass(Monster.class, new AABB(actual).inflate(6)).isEmpty(), "Bright Cursed Earth should stop spawning mobs");
            test.succeed();
        });
    }
    @GameTest(template = "machine_test")
    public static void sunlightRevertsCursedEarthAndReleasesItsExistingInventory(GameTestHelper test) {
        var level = test.getLevel();
        var pos = new BlockPos(3, 2, 3);
        var actual = test.absolutePos(pos).atY(128);
        var cursed = ExtrautilitiesrebirthModBlocks.CURSED_EARTH.get();
        level.setBlockAndUpdate(actual, cursed.defaultBlockState());
        var sample = new ItemStack(Items.IRON_INGOT, 7).setHoverName(Component.literal("Legacy ore"));
        if (level.getBlockEntity(actual) instanceof Container inventory) {
            inventory.setItem(2, sample.copy());
        } else {
            throw new AssertionError("The legacy container should still exist");
        }
        test.runAfterDelay(15, () -> {
            var originalTime = level.getDayTime();
            try {
                level.setDayTime(6_000);
                level.updateSkyBrightness();
                test.assertTrue(level.canSeeSky(actual.above()) && level.getMaxLocalRawBrightness(actual.above()) >= 9 && level.isDay(),
                        "The open surface should receive daylight, sky=" + level.canSeeSky(actual.above())
                                + ", brightness=" + level.getMaxLocalRawBrightness(actual.above()) + ", day=" + level.isDay());
                level.getBlockState(actual).tick(level, actual, RandomSource.create(749));
            } finally {
                level.setDayTime(originalTime);
                level.updateSkyBrightness();
            }
            test.assertTrue(level.getBlockState(actual).is(Blocks.DIRT), "Sunlight should revert Cursed Earth to dirt");
            test.assertTrue(level.getEntities(EntityType.ITEM, new AABB(actual).inflate(2), item -> ItemStack.matches(item.getItem(), sample)).size() == 1,
                    "Converting an existing Cursed Earth block must preserve its stored items");
            level.setBlockAndUpdate(actual, Blocks.AIR.defaultBlockState());
            test.succeed();
        });
    }

}
