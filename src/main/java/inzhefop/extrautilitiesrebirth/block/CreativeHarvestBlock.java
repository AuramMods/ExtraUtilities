package inzhefop.extrautilitiesrebirth.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class CreativeHarvestBlock extends Block {
    public CreativeHarvestBlock() {
        super(Properties.of().sound(SoundType.STONE).strength(-1, 3600000));
    }

    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 15;
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                level.sendBlockUpdated(pos, state, state, UPDATE_CLIENTS);
            }
            return false;
        }
        boolean removed = super.onDestroyedByPlayer(state, level, pos, player, willHarvest, fluid);
        if (removed && !level.isClientSide) {
            popResource(level, pos, new ItemStack(this));
        }
        return removed;
    }
}
