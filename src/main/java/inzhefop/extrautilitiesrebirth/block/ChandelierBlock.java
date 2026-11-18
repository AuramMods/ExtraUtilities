
package inzhefop.extrautilitiesrebirth.block;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import inzhefop.extrautilitiesrebirth.world.ChunkBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;



import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;

import java.util.List;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;

@Mod.EventBusSubscriber(modid = ExtrautilitiesrebirthMod.MODID)
public class ChandelierBlock extends Block {
	private static final int SPAWN_RADIUS = 16;
	private static final Map<ServerLevel, Map<ChunkPos, Set<BlockPos>>> CHANDELIERS = new WeakHashMap<>();

	public ChandelierBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(1f, 15f).lightLevel(s -> 15).noOcclusion()
				.isRedstoneConductor((bs, br, bp) -> false));
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(state, level, pos, oldState, moving);
		if (level instanceof ServerLevel serverLevel) {
			CHANDELIERS.computeIfAbsent(serverLevel, ignored -> new HashMap<>())
					.computeIfAbsent(new ChunkPos(pos), ignored -> new HashSet<>()).add(pos.immutable());
		}
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (level instanceof ServerLevel serverLevel && !newState.is(this)) {
			var chunks = CHANDELIERS.get(serverLevel);
			if (chunks != null) {
				var chunkPos = new ChunkPos(pos);
				var positions = chunks.get(chunkPos);
				if (positions != null && positions.remove(pos) && positions.isEmpty()) {
					chunks.remove(chunkPos);
				}
			}
		}
		super.onRemove(state, level, pos, newState, moving);
	}

	@SubscribeEvent
	public static void loadChandeliers(ChunkEvent.Load event) {
		if (event.getLevel() instanceof ServerLevel level && event.getChunk() instanceof LevelChunk chunk) {
			var positions = new HashSet<BlockPos>();
			ChunkBlocks.forEach(chunk, ExtrautilitiesrebirthModBlocks.CHANDELIER.get(), positions::add);
			if (!positions.isEmpty()) {
				CHANDELIERS.computeIfAbsent(level, ignored -> new HashMap<>()).put(chunk.getPos(), positions);
			}
		}
	}

	@SubscribeEvent
	public static void unloadChandeliers(ChunkEvent.Unload event) {
		if (event.getLevel() instanceof ServerLevel level) {
			var chunks = CHANDELIERS.get(level);
			if (chunks != null) {
				chunks.remove(event.getChunk().getPos());
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void suppressNearbyNaturalSpawns(MobSpawnEvent.PositionCheck event) {
		if (event.getSpawnType() != MobSpawnType.NATURAL || event.getEntity().getType().getCategory() != MobCategory.MONSTER
				|| !(event.getLevel() instanceof ServerLevel level)) {
			return;
		}
		var chunks = CHANDELIERS.get(level);
		if (chunks == null || chunks.isEmpty()) {
			return;
		}
		var spawn = event.getEntity().blockPosition();
		var center = new ChunkPos(spawn);
		for (int x = center.x - 1; x <= center.x + 1; x++) {
			for (int z = center.z - 1; z <= center.z + 1; z++) {
				var positions = chunks.get(new ChunkPos(x, z));
				if (positions != null && positions.stream().anyMatch(pos -> pos.distSqr(spawn) <= SPAWN_RADIUS * SPAWN_RADIUS)) {
					event.setResult(Event.Result.DENY);
					return;
				}
			}
		}
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return true;
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}

	@Override
	public boolean canSurvive(BlockState blockstate, LevelReader worldIn, BlockPos pos) {
		BlockPos ceiling = pos.above();
		return worldIn.getBlockState(ceiling).isFaceSturdy(worldIn, ceiling, Direction.DOWN);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos currentPos,
			BlockPos facingPos) {
		return !state.canSurvive(world, currentPos)
				? Blocks.AIR.defaultBlockState()
				: super.updateShape(state, facing, facingState, world, currentPos, facingPos);
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> dropsOriginal = super.getDrops(state, builder);
		if (!dropsOriginal.isEmpty())
			return dropsOriginal;
		return Collections.singletonList(new ItemStack(this, 1));
	}

	@OnlyIn(Dist.CLIENT)
	public static void registerRenderLayer() {
		ItemBlockRenderTypes.setRenderLayer(ExtrautilitiesrebirthModBlocks.CHANDELIER.get(), renderType -> renderType == RenderType.cutout());
	}
}
