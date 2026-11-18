package inzhefop.extrautilitiesrebirth.procedures;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import java.util.List;

public class CursedEarthUpdateTickProcedure {
    private static final int LIGHT_LIMIT = 9;
    private static final int SPREAD_LIMIT = 10;
    private static final int NEARBY_MONSTER_LIMIT = 8;
    private static final String CYCLE = "cycle";
    private static final List<Direction> SPREAD_DIRECTIONS = List.of(Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH);
    private static final List<EntityType<? extends Monster>> MONSTERS = List.of(EntityType.CREEPER, EntityType.BLAZE, EntityType.SPIDER, EntityType.SKELETON);

    public static void tick(ServerLevel level, BlockPos pos, RandomSource random) {
        var surface = pos.above();
        if (level.getMaxLocalRawBrightness(surface) >= LIGHT_LIMIT) {
            if (level.dimensionType().hasSkyLight() && level.canSeeSky(surface) && level.isDay()) {
                level.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
            }
            return;
        }
        spread(level, pos);
        var roll = random.nextInt(100);
        if (roll < MONSTERS.size()) {
            trySpawn(level, surface, random, MONSTERS.get(roll));
        }
    }

    private static void spread(ServerLevel level, BlockPos pos) {
        var source = level.getBlockEntity(pos);
        if (source == null) {
            return;
        }
        var data = source.getPersistentData();
        int cycle = data.getInt(CYCLE);
        if (cycle >= SPREAD_LIMIT) {
            return;
        }
        data.putDouble(CYCLE, ++cycle);
        source.setChanged();
        for (Direction direction : SPREAD_DIRECTIONS) {
            var neighbor = pos.relative(direction);
            var substrate = level.getBlockState(neighbor);
            if ((substrate.is(Blocks.GRASS_BLOCK) || substrate.is(Blocks.DIRT)) && level.isEmptyBlock(neighbor.above())
                    && level.getMaxLocalRawBrightness(neighbor.above()) < LIGHT_LIMIT
                    && level.setBlockAndUpdate(neighbor, ExtrautilitiesrebirthModBlocks.CURSED_EARTH.get().defaultBlockState())) {
                var next = level.getBlockEntity(neighbor);
                if (next != null) {
                    next.getPersistentData().putDouble(CYCLE, cycle);
                    next.setChanged();
                }
                return;
            }
        }
    }

    private static void trySpawn(ServerLevel level, BlockPos surface, RandomSource random, EntityType<? extends Monster> type) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || !level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)
                || !level.isEmptyBlock(surface)) {
            return;
        }
        var area = new AABB(surface).inflate(7, 4, 7);
        if (level.getEntitiesOfClass(Mob.class, area, mob -> mob.getType().getCategory() == MobCategory.MONSTER).size() >= NEARBY_MONSTER_LIMIT) {
            return;
        }
        var mob = type.create(level);
        if (mob == null) {
            return;
        }
        var center = Vec3.atBottomCenterOf(surface);
        mob.moveTo(center.x, center.y, center.z, random.nextFloat() * 360, 0);
        if (level.noCollision(mob) && ForgeEventFactory.checkSpawnPosition(mob, level, MobSpawnType.TRIGGERED)) {
            ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(surface), MobSpawnType.TRIGGERED, null, null);
            level.tryAddFreshEntityWithPassengers(mob);
        }
    }
}
