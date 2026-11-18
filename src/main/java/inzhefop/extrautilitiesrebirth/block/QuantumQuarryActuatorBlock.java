
package inzhefop.extrautilitiesrebirth.block;

import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;


import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import inzhefop.extrautilitiesrebirth.block.entity.QuantumQuarryActuatorBlockEntity;
import inzhefop.extrautilitiesrebirth.block.entity.QuantumQuarryBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Collections;


public class QuantumQuarryActuatorBlock extends Block implements EntityBlock {
	public static final DirectionProperty FACING = DirectionalBlock.FACING;

	public QuantumQuarryActuatorBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(2f, 800f).requiresCorrectToolForDrops());
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}
 
	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 15;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
	}

	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}

	@Override
	public boolean canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player) {
		if (player.getInventory().getSelected().getItem() instanceof TieredItem tieredItem)
			return tieredItem.getTier().getLevel() >= 3;
		return false;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> dropsOriginal = super.getDrops(state, builder);
		if (!dropsOriginal.isEmpty())
			return dropsOriginal;
		return Collections.singletonList(new ItemStack(this, 1));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new QuantumQuarryActuatorBlockEntity(pos, state);
	}

	@Nullable
	public static QuantumQuarryBlockEntity findQuarry(Level level, BlockPos pos, BlockState state) {
		Direction inward = state.getValue(FACING).getOpposite();
		if (level.getBlockEntity(pos.relative(inward)) instanceof QuantumQuarryBlockEntity quarry) {
			return quarry;
		}
		// Existing worlds could assemble actuators with any orientation.
		QuantumQuarryBlockEntity nearby = null;
		for (Direction direction : Direction.values()) {
			if (level.getBlockEntity(pos.relative(direction)) instanceof QuantumQuarryBlockEntity quarry) {
				if (nearby != null) {
					return null;
				}
				nearby = quarry;
			}
		}
		return nearby;
	}

	@Override
	public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (player instanceof ServerPlayer serverPlayer) {
			QuantumQuarryBlockEntity quarry = findQuarry(level, pos, state);
			if (quarry != null) {
				QuantumQuarryBlock.openQuarry(serverPlayer, quarry);
			}
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}
}
