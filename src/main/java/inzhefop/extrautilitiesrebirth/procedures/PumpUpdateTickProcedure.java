package inzhefop.extrautilitiesrebirth.procedures;

import inzhefop.extrautilitiesrebirth.block.entity.EnderThermicPumpBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class PumpUpdateTickProcedure {
    static final int ACTIVE = 1;
    static final int FINISHED = 2;
    static final int BLOCKED = 3;
    private static final int DEFAULT_FE_PER_STEP = 260;

    public static void execute(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof EnderThermicPumpBlockEntity pump)) {
            return;
        }
        exportFluid(level, pump);

        CompoundTag data = pump.getPersistentData();
        int state = data.getInt("state");
        if (state != ACTIVE && state != BLOCKED) {
            return;
        }

        double speed = Math.max(1, data.getDouble("eqspeed"));
        double multiplier = Math.pow(2, Math.round(speed) * 0.15);
        int iterations = (int) multiplier;
        int baseCost = data.contains("eqpowerdrain") ? data.getInt("eqpowerdrain") : DEFAULT_FE_PER_STEP;
        int energyCost = Math.max(1, (int) (baseCost * multiplier));
        var energy = pump.getCapability(ForgeCapabilities.ENERGY).orElseThrow(IllegalStateException::new);
        var tank = pump.tank();
        for (int step = 0; step < iterations; step++) {
            BlockPos target = BlockPos.containing(data.getDouble("xcoord"), data.getDouble("ycoord"), data.getDouble("zcoord"));
            if (!level.hasChunkAt(target)) {
                return;
            }

            FluidStack bucket = bucketAt(level.getBlockState(target), data);
            if (tank.getSpace() <= 0 || (!bucket.isEmpty()
                    && tank.fill(bucket, IFluidHandler.FluidAction.SIMULATE) != FluidType.BUCKET_VOLUME)) {
                setState(pump, BLOCKED);
                return;
            }

            setState(pump, ACTIVE);
            if (energy.extractEnergy(energyCost, true) != energyCost) {
                return;
            }
            energy.extractEnergy(energyCost, false);
            double ticks = data.getDouble("ticks") + 1;
            if (ticks < 20 / speed) {
                data.putDouble("ticks", ticks);
                pump.setChanged();
                continue;
            }

            if (!bucket.isEmpty()) {
                BlockState replacement = data.getBoolean("eqhole") ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState();
                if (!level.setBlock(target, replacement, Block.UPDATE_ALL)) {
                    return;
                }
                tank.fill(bucket, IFluidHandler.FluidAction.EXECUTE);
                data.putBoolean(bucket.getFluid() == Fluids.LAVA ? "lava" : "water", true);
            }

            data.putDouble("ticks", 0);
            data.putDouble("minedblocks", data.getDouble("minedblocks") + 1);
            if (EnderQuarryUpdateTickProcedure.advanceCursor(data, level.getMinBuildHeight())) {
                setState(pump, FINISHED);
                return;
            }
            pump.setChanged();
        }
        exportFluid(level, pump);
    }

    private static FluidStack bucketAt(BlockState state, CompoundTag data) {
        if (!(state.getBlock() instanceof LiquidBlock) || !state.getFluidState().isSource()) {
            return FluidStack.EMPTY;
        }
        var fluid = state.getFluidState().getType();
        if (fluid == Fluids.WATER && !data.getBoolean("lava") || fluid == Fluids.LAVA && !data.getBoolean("water")) {
            return new FluidStack(fluid, FluidType.BUCKET_VOLUME);
        }
        return FluidStack.EMPTY;
    }

    private static void exportFluid(ServerLevel level, EnderThermicPumpBlockEntity pump) {
        var tank = pump.tank();
        if (tank.isEmpty()) {
            return;
        }
        var receiver = level.getBlockEntity(pump.getBlockPos().above());
        if (receiver == null) {
            return;
        }
        receiver.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.DOWN).ifPresent(destination -> {
            if (destination != tank) {
                FluidStack fluid = tank.getFluid().copy();
                int moved = destination.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
                if (moved > 0) {
                    tank.drain(moved, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        });
    }

    private static void setState(EnderThermicPumpBlockEntity pump, int state) {
        CompoundTag data = pump.getPersistentData();
        if (data.getInt("state") != state) {
            data.putDouble("state", state);
            pump.setChanged();
        }
    }
}
