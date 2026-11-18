package inzhefop.extrautilitiesrebirth.procedures;

import inzhefop.extrautilitiesrebirth.block.entity.MagmaGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

public class MagmaGeneratorUpdateTickProcedure {
    private static final double BURN_TIME = 100;
    private static final int ENERGY_PER_BURN_TICK = 20;

    public static void execute(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof MagmaGeneratorBlockEntity generator)) {
            return;
        }

        CompoundTag data = generator.getPersistentData();
        double time = Math.max(0, data.getDouble("time"));
        double speed = generator.getSpeedMultiplier();
        boolean hasLava = generator.hasLavaBucket();
        boolean canRun = !data.getBoolean("redinv") || level.hasNeighborSignal(pos);
        int generated = 0;

        if (canRun && (time > 0 || hasLava)) {
            double availableTime = time > 0 ? time : BURN_TIME;
            double consumedTime = Math.min(availableTime, speed);
            int amount = (int) Math.round(ENERGY_PER_BURN_TICK * consumedTime);
            if (generator.hasSpaceForEnergy(amount)) {
                if (time <= 0) {
                    generator.consumeLavaBucket();
                }
                time = availableTime - consumedTime;
                if (time < 1e-9) {
                    time = 0;
                }
                data.putDouble("time", time);
                generated = GeneratorOutput.storeGeneratedEnergy(level, pos, amount);
                UpdateRGBProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
                hasLava = generator.hasLavaBucket();
            }
        }

        boolean valid = hasLava || time > 0;
        int progress = (int) (time / BURN_TIME * 22);
        if (valid != data.getBoolean("valid") || progress != data.getInt("progress2")) {
            data.putBoolean("valid", valid);
            data.putInt("progress2", progress);
            var state = generator.getBlockState();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }
        if (generated > 0) {
            generator.setChanged();
        }
        GeneratorOutput.finishTick(level, pos, generated, speed);
    }
}
