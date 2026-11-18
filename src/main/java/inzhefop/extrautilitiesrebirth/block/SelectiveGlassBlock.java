package inzhefop.extrautilitiesrebirth.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

abstract class SelectiveGlassBlock extends Block {
    private final boolean inverted;
    private final boolean blocksLight;

    protected SelectiveGlassBlock(boolean inverted, boolean blocksLight) {
        super(Properties.of().sound(SoundType.GLASS).strength(0.1f, 15f).noOcclusion().dynamicShape()
                .isRedstoneConductor((state, level, pos) -> false));
        this.inverted = inverted;
        this.blocksLight = blocksLight;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext collision && collision.getEntity() != null) {
            boolean standingPlayer = collision.getEntity() instanceof Player player && !player.isShiftKeyDown();
            if (standingPlayer != inverted) {
                return Shapes.empty();
            }
        }
        return Shapes.block();
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return type == PathComputationType.WATER ? super.isPathfindable(state, level, pos, type) : inverted;
    }

    @Override
    public boolean skipRendering(BlockState state, BlockState neighbor, Direction side) {
        return neighbor.is(this) || super.skipRendering(state, neighbor, side);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return !blocksLight;
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return blocksLight ? 15 : 0;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder loot) {
        List<ItemStack> drops = super.getDrops(state, loot);
        return drops.isEmpty() ? List.of(new ItemStack(this)) : drops;
    }
}
