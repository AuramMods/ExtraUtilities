
package inzhefop.extrautilitiesrebirth.block;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import inzhefop.extrautilitiesrebirth.world.ChunkBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;



import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;

@Mod.EventBusSubscriber(modid = ExtrautilitiesrebirthMod.MODID)
public class ChunkLoadingWandBlock extends Block implements SimpleWaterloggedBlock {
	public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
	public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	public ChunkLoadingWandBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.WOOD).strength(5f, 50f).noOcclusion()
				.isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
	}

	public static void registerTicketValidation(FMLCommonSetupEvent event) {
		event.enqueueWork(() -> ForgeChunkManager.setForcedChunkLoadingCallback(ExtrautilitiesrebirthMod.MODID, (level, tickets) -> {
			for (BlockPos owner : tickets.getBlockTickets().keySet()) {
				if (!level.getBlockState(owner).is(ExtrautilitiesrebirthModBlocks.CHUNK_LOADING_WAND.get())) {
					tickets.removeAllTickets(owner);
				}
			}
		}));
	}

	private static void setTicket(ServerLevel level, BlockPos owner, boolean add) {
		ChunkPos chunk = new ChunkPos(owner);
		ForgeChunkManager.forceChunk(level, ExtrautilitiesrebirthMod.MODID, owner, chunk.x, chunk.z, add, true);
	}

	@Override
	public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
		super.onPlace(state, level, pos, oldState, moving);
		if (level instanceof ServerLevel serverLevel && !oldState.is(this)) {
			setTicket(serverLevel, pos, true);
		}
	}

	@Override
	public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
		if (level instanceof ServerLevel serverLevel && !newState.is(this)) {
			setTicket(serverLevel, pos, false);
		}
		super.onRemove(state, level, pos, newState, moving);
	}

	@SubscribeEvent
	public static void restoreLegacyWands(ChunkEvent.Load event) {
		if (event.isNewChunk() || !(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) {
			return;
		}
		var wand = ExtrautilitiesrebirthModBlocks.CHUNK_LOADING_WAND.get();
		var owners = new ArrayList<BlockPos>();
		ChunkBlocks.forEach(chunk, wand, owners::add);
		if (!owners.isEmpty()) {
			// ChunkEvent.Load can run before promotion to FULL. Add tickets on the server queue.
			var server = level.getServer();
			server.tell(new TickTask(server.getTickCount(), () -> {
				for (BlockPos owner : owners) {
					if (level.getBlockState(owner).is(wand)) {
						setTicket(level, owner, true);
					}
				}
			}));
		}
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return state.getFluidState().isEmpty();
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, WATERLOGGED);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		boolean flag = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
		return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(WATERLOGGED, flag);
	}

	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}

	@Override
	public FluidState getFluidState(BlockState state) {
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor world, BlockPos currentPos,
			BlockPos facingPos) {
		if (state.getValue(WATERLOGGED)) {
			world.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
		}
		return super.updateShape(state, facing, facingState, world, currentPos, facingPos);
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
		ItemBlockRenderTypes.setRenderLayer(ExtrautilitiesrebirthModBlocks.CHUNK_LOADING_WAND.get(), renderType -> renderType == RenderType.cutout());
	}
}
