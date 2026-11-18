package inzhefop.extrautilitiesrebirth.block;

import inzhefop.extrautilitiesrebirth.block.entity.RedstoneClockOffBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneClockOffBlock extends RedstoneClockBlock {
    public RedstoneClockOffBlock() {
        super(Properties.of().sound(SoundType.STONE).strength(0.7f, 10f).requiresCorrectToolForDrops(), 0);
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return player.getMainHandItem().getItem() instanceof TieredItem tool && tool.getTier().getLevel() >= 1;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneClockOffBlockEntity(pos, state);
    }
}
