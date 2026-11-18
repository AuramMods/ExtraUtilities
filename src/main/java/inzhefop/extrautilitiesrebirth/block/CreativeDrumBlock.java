
package inzhefop.extrautilitiesrebirth.block;

import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.block.entity.CreativeDrumBlockEntity;

public class CreativeDrumBlock extends DrumBlock {
	public CreativeDrumBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(-1, 3600000).noOcclusion()
				.isRedstoneConductor((bs, br, bp) -> false));
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
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {

		return Shapes.or(box(2, 0, 2, 14, 1, 14), box(4, 1, 4, 10, 15, 10), box(2, 1, 2, 14, 16, 14));
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CreativeDrumBlockEntity(pos, state);
	}

	@OnlyIn(Dist.CLIENT)
	public static void registerRenderLayer() {
		ItemBlockRenderTypes.setRenderLayer(ExtrautilitiesrebirthModBlocks.CREATIVE_DRUM.get(), renderType -> renderType == RenderType.cutout());
	}
}
