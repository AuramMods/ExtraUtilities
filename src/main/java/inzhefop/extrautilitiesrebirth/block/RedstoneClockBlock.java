package inzhefop.extrautilitiesrebirth.block;

import inzhefop.extrautilitiesrebirth.block.entity.RedstoneClockBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

public class RedstoneClockBlock extends Block implements EntityBlock {
    private final int signal;

    public RedstoneClockBlock() {
        this(Properties.of().sound(SoundType.STONE).strength(0.7f, 10f), 15);
    }

    protected RedstoneClockBlock(Properties properties, int signal) {
        super(properties);
        this.signal = signal;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return signal;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return signal;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder loot) {
        List<ItemStack> drops = super.getDrops(state, loot);
        return drops.isEmpty() ? List.of(new ItemStack(this)) : drops;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneClockBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (world, pos, blockState, entity) -> {
            if (entity instanceof RedstoneClockBlockEntity clock) {
                clock.advancePhase();
            }
        };
    }
}
