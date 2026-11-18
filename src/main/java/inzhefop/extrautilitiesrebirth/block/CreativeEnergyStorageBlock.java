package inzhefop.extrautilitiesrebirth.block;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Collections;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import inzhefop.extrautilitiesrebirth.procedures.GeneratorOutput;
import inzhefop.extrautilitiesrebirth.block.entity.CreativeEnergyStorageBlockEntity;
import net.minecraft.world.level.storage.loot.LootParams;

public class CreativeEnergyStorageBlock extends Block implements EntityBlock {
	public CreativeEnergyStorageBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(-1, 3600000));
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 15;
	}

	@Override
	public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
		List<ItemStack> dropsOriginal = super.getDrops(state, builder);
		if (!dropsOriginal.isEmpty())
			return dropsOriginal;
		return Collections.singletonList(new ItemStack(this, 1));
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level instanceof ServerLevel server && type == ExtrautilitiesrebirthModBlockEntities.CREATIVE_ENERGY_STORAGE.get()) {
			return (world, pos, blockState, entity) -> GeneratorOutput.transferStoredEnergy(server, entity, Integer.MAX_VALUE);
		}
		return null;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CreativeEnergyStorageBlockEntity(pos, state);
	}
}
