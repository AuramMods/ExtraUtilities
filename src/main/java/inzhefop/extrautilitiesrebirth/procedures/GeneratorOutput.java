package inzhefop.extrautilitiesrebirth.procedures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

final class GeneratorOutput {
    private static final int TRANSFER_LIMIT = 100_000;
    private static final Direction[] OUTPUT_SIDES = {
            Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH, Direction.UP, Direction.DOWN
    };

    private GeneratorOutput() {
    }

    static int storeGeneratedEnergy(LevelAccessor world, BlockPos pos, int amount) {
        if (world.isClientSide() || amount <= 0) {
            return 0;
        }
        BlockEntity entity = world.getBlockEntity(pos);
        return entity == null ? 0 : entity.getCapability(ForgeCapabilities.ENERGY)
                .map(storage -> storage.receiveEnergy(amount, false)).orElse(0);
    }

    static void syncBurnProgress(ServerLevel level, BlockEntity generator, boolean valid, double time, double duration) {
        CompoundTag data = generator.getPersistentData();
        int progress = duration > 0 ? (int) (time / duration * 22) : 0;
        if (valid != data.getBoolean("valid") || progress != data.getInt("progress2")) {
            data.putBoolean("valid", valid);
            data.putInt("progress2", progress);
            BlockState state = generator.getBlockState();
            level.sendBlockUpdated(generator.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
        }
    }

    static void finishTick(LevelAccessor world, BlockPos pos, int generated, double speed) {
        if (!(world instanceof ServerLevel level)) {
            return;
        }
        BlockEntity generator = level.getBlockEntity(pos);
        if (generator == null) {
            return;
        }

        generator.getCapability(ForgeCapabilities.ENERGY).ifPresent(source -> transfer(level, pos, source));
        CompoundTag data = generator.getPersistentData();
        long seconds = (long) Math.ceil(Math.max(0, data.getDouble("time")) / Math.max(1, speed) / 20);
        String production = "Output: " + generated + " FE/t";
        String time = seconds + "s";
        if (!production.equals(data.getString("production")) || !time.equals(data.getString("seconds"))) {
            data.putString("production", production);
            data.putString("seconds", time);
            generator.setChanged();
            BlockState state = generator.getBlockState();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private static void transfer(ServerLevel level, BlockPos pos, IEnergyStorage source) {
        for (Direction side : OUTPUT_SIDES) {
            int available = source.extractEnergy(TRANSFER_LIMIT, true);
            if (available <= 0) {
                return;
            }
            BlockEntity neighbor = level.getBlockEntity(pos.relative(side));
            if (neighbor == null) {
                continue;
            }
            IEnergyStorage receiver = neighbor.getCapability(ForgeCapabilities.ENERGY, side.getOpposite())
                    .orElse(null);
            if (receiver != null && receiver != source && receiver.canReceive()) {
                int accepted = receiver.receiveEnergy(available, false);
                if (accepted > 0) {
                    source.extractEnergy(accepted, false);
                }
            }
        }
    }
}
