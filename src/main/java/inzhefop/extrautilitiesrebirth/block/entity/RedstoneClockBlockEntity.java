package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneClockBlockEntity extends BlockEntity {
    private static final int PHASE_LENGTH = 18;
    private static final String TICKS_TAG = "ticks";

    public RedstoneClockBlockEntity(BlockPos pos, BlockState state) {
        this(ExtrautilitiesrebirthModBlockEntities.REDSTONE_CLOCK.get(), pos, state);
    }

    protected RedstoneClockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public void advancePhase() {
        CompoundTag data = getPersistentData();
        int ticks = Math.max(0, data.getInt(TICKS_TAG));
        if (ticks < PHASE_LENGTH - 1) {
            data.putDouble(TICKS_TAG, ticks + 1);
            level.blockEntityChanged(worldPosition);
            return;
        }

        Block active = ExtrautilitiesrebirthModBlocks.REDSTONE_CLOCK.get();
        Block next = getBlockState().is(active) ? ExtrautilitiesrebirthModBlocks.REDSTONE_CLOCK_OFF.get() : active;
        CompoundTag nextData = data.copy();
        nextData.putDouble(TICKS_TAG, 0);
        if (level.setBlockAndUpdate(worldPosition, next.defaultBlockState())
                && level.getBlockEntity(worldPosition) instanceof RedstoneClockBlockEntity clock) {
            clock.getPersistentData().merge(nextData);
            clock.setChanged();
        }
    }
}
