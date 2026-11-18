package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import inzhefop.extrautilitiesrebirth.network.ExtrautilitiesrebirthModVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ExtrautilitiesrebirthMod.MODID)
@PrefixGameTestTemplate(false)
public class AccessGameTests {
    @GameTest(template = "machine_test")
    public static void angelBlockPlacesInAirUsingTheActualHand(GameTestHelper test) {
        var level = test.getLevel();
        var player = test.makeMockSurvivalPlayer();
        var start = test.absolutePos(new BlockPos(3, 2, 1));
        player.moveTo(start.getX() + 0.5, start.getY(), start.getZ() + 0.5, 0, 0);
        var target = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(3)));
        var angel = ExtrautilitiesrebirthModBlocks.ANGEL_BLOCK.get();
        var stack = new ItemStack(angel, 2);
        player.setItemInHand(InteractionHand.OFF_HAND, stack);
        test.assertTrue(stack.use(level, player, InteractionHand.OFF_HAND).getResult().consumesAction(),
                "An offhand Angel Block should place even without a support block");
        test.assertTrue(level.getBlockState(target).is(angel) && stack.getCount() == 1,
                "Air placement should create one block at the reach point and consume one item");
        level.setBlockAndUpdate(target, Blocks.STONE.defaultBlockState());
        test.assertTrue(!stack.use(level, player, InteractionHand.OFF_HAND).getResult().consumesAction()
                && level.getBlockState(target).is(Blocks.STONE) && stack.getCount() == 1,
                "Air placement must never overwrite a block or lose an item on failure");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void chandelierNeedsSturdyCeilingAndDropsWhenUnsupported(GameTestHelper test) {
        var level = test.getLevel();
        var pos = new BlockPos(3, 2, 3);
        var actual = test.absolutePos(pos);
        var chandelier = ExtrautilitiesrebirthModBlocks.CHANDELIER.get();
        test.setBlock(pos.above(), Blocks.WATER);
        test.assertTrue(!chandelier.defaultBlockState().canSurvive(level, actual), "A liquid is not a ceiling");
        test.setBlock(pos.above(), Blocks.STONE);
        var player = test.makeMockSurvivalPlayer();
        var stack = new ItemStack(chandelier);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var clicked = actual.above();
        var hit = new BlockHitResult(Vec3.atBottomCenterOf(clicked), Direction.DOWN, clicked, false);
        test.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(),
                "The chandelier should place on the underside of a ceiling");
        test.assertBlockPresent(chandelier, pos);
        test.assertTrue(level.getBlockState(actual).getLightEmission(level, actual) == 15, "The chandelier should be a full light source");
        test.setBlock(pos.above(), Blocks.AIR);
        test.assertBlockPresent(Blocks.AIR, pos);
        test.assertTrue(test.getEntities(EntityType.ITEM, pos, 2)
                .stream().anyMatch(item -> item.getItem().is(chandelier.asItem())), "The chandelier should drop when the ceiling breaks");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void chandelierStopsNaturalMonstersWithinSixteenBlocks(GameTestHelper test) {
        var level = test.getLevel();
        var pos = new BlockPos(3, 2, 3);
        var actual = test.absolutePos(pos);
        test.setBlock(pos.above(), Blocks.STONE);
        test.setBlock(pos.east(), Blocks.STONE);
        test.setBlock(pos, ExtrautilitiesrebirthModBlocks.CHANDELIER.get());
        var zombie = EntityType.ZOMBIE.create(level);
        zombie.moveTo(Vec3.atBottomCenterOf(actual.east(16)));
        var natural = new MobSpawnEvent.PositionCheck(zombie, level, MobSpawnType.NATURAL, null);
        MinecraftForge.EVENT_BUS.post(natural);
        test.assertTrue(natural.getResult() == Event.Result.DENY, "A chandelier should block a natural monster through walls within 16 blocks");
        var spawner = new MobSpawnEvent.PositionCheck(zombie, level, MobSpawnType.SPAWNER, null);
        MinecraftForge.EVENT_BUS.post(spawner);
        test.assertTrue(spawner.getResult() != Event.Result.DENY, "The chandelier must leave spawner mobs alone");
        zombie.moveTo(Vec3.atBottomCenterOf(actual.east(17)));
        var distant = new MobSpawnEvent.PositionCheck(zombie, level, MobSpawnType.NATURAL, null);
        MinecraftForge.EVENT_BUS.post(distant);
        test.assertTrue(distant.getResult() != Event.Result.DENY, "The chandelier's radius should stop at 16 blocks");
        test.setBlock(pos.above(), Blocks.AIR);
        zombie.moveTo(Vec3.atBottomCenterOf(actual.east(16)));
        var removed = new MobSpawnEvent.PositionCheck(zombie, level, MobSpawnType.NATURAL, null);
        MinecraftForge.EVENT_BUS.post(removed);
        test.assertTrue(removed.getResult() != Event.Result.DENY, "A removed chandelier should stop blocking spawns");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void wandBlocksOwnIndependentTickingChunkTickets(GameTestHelper test) {
        var level = test.getLevel();
        var first = new BlockPos(3, 2, 3);
        var second = new BlockPos(4, 2, 3);
        var wand = ExtrautilitiesrebirthModBlocks.CHUNK_LOADING_WAND.get();
        var saveData = level.getDataStorage().computeIfAbsent(ForcedChunksSavedData::load, ForcedChunksSavedData::new, "chunks");
        var tickets = saveData.getBlockForcedChunks().getTickingChunks();
        var chunk = new ChunkPos(test.absolutePos(first)).toLong();
        test.assertTrue(new ChunkPos(test.absolutePos(second)).toLong() == chunk, "The fixtures must occupy one chunk");
        var initial = tickets.values().stream().filter(chunks -> chunks.contains(chunk)).count();
        test.setBlock(first, wand);
        test.setBlock(second, wand);
        test.assertTrue(tickets.values().stream().filter(chunks -> chunks.contains(chunk)).count() == initial + 2,
                "Each wand should create a separate ticking ticket for the chunk");
        test.setBlock(first, Blocks.AIR);
        test.assertTrue(tickets.values().stream().filter(chunks -> chunks.contains(chunk)).count() == initial + 1,
                "Breaking one wand should leave the other wand's ticket active");
        test.setBlock(second, Blocks.AIR);
        test.assertTrue(tickets.values().stream().filter(chunks -> chunks.contains(chunk)).count() == initial,
                "Breaking the final wand should remove both tickets");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void portalAndGlobeStoreOverworldReturnPositionUsingTheirInteractions(GameTestHelper test) {
        var level = test.getLevel();
        var player = test.makeMockSurvivalPlayer();
        var pos = new BlockPos(3, 2, 3);
        var absolute = test.absolutePos(pos);
        var portal = ExtrautilitiesrebirthModBlocks.OVERWORLD_PORTAL.get();
        test.setBlock(pos, portal);
        var state = level.getBlockState(absolute);
        var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        test.assertTrue(state.use(level, player, InteractionHand.MAIN_HAND, hit).consumesAction(), "An Overworld Portal should accept a right-click");
        var data = player.getCapability(ExtrautilitiesrebirthModVariables.PLAYER_VARIABLES_CAPABILITY).orElseThrow(AssertionError::new);
        var saved = new ExtrautilitiesrebirthModVariables.PlayerVariables();
        saved.readNBT(data.writeNBT());
        test.assertTrue(saved.overworld_portal_x == absolute.getX() && saved.overworld_portal_y == absolute.getY() + 1
                && saved.overworld_portal_z == absolute.getZ(), "The space above the portal should be persisted as the return target");
        var newPos = test.absolutePos(pos.east());
        player.moveTo(newPos.getX() + 0.5, newPos.getY(), newPos.getZ() + 0.5, 0, 0);
        var globe = new ItemStack(ExtrautilitiesrebirthModItems.PORTABLE_GLOBE.get());
        player.setItemInHand(InteractionHand.OFF_HAND, globe);
        test.assertTrue(globe.use(level, player, InteractionHand.OFF_HAND).getResult().consumesAction(), "The offhand globe should accept a right-click");
        test.assertTrue(newPos.equals(data.getOverworldReturn()), "The Portable Globe should save the player's feet on an Overworld right-click");

        var legacy = new CompoundTag();
        legacy.putDouble("overworld_portal_x", 16);
        legacy.putDouble("overworld_portal_y", -40);
        legacy.putDouble("overworld_portal_z", 8);
        saved.readNBT(legacy);
        test.assertTrue(new BlockPos(16, -40, 8).equals(saved.getOverworldReturn()), "A negative return height should survive a legacy save upgrade");
        saved.readNBT(new CompoundTag());
        test.assertTrue(saved.getOverworldReturn() == null, "An untouched save should use the world spawn fallback");
        data.setOverworldReturn(BlockPos.ZERO);
        saved.readNBT(data.writeNBT());
        test.assertTrue(BlockPos.ZERO.equals(saved.getOverworldReturn()), "A new anchor at the origin should survive saving");
        test.succeed();
    }
}
