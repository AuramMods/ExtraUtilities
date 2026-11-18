package inzhefop.extrautilitiesrebirth.procedures;

import inzhefop.extrautilitiesrebirth.block.entity.FuelGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

public final class FuelGeneratorTick {
    private FuelGeneratorTick() {
    }

    public static void execute(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FuelGeneratorBlockEntity generator)) {
            return;
        }

        CompoundTag data = generator.getPersistentData();
        ItemStack fuel = generator.getFuelStack();
        var type = generator.getFuelType();
        int fuelDuration = type.burnDuration(fuel);
        double time = Math.max(0, data.getDouble("time"));
        double speed = generator.getSpeedMultiplier();
        int duration = data.getInt("burnDuration");
        int generated = 0;

        boolean enabled = !data.getBoolean("redinv") || level.hasNeighborSignal(pos);
        if (enabled && (time > 0 || fuelDuration > 0)) {
            boolean starting = time == 0;
            int power = data.getInt("burnPower");
            if (starting || power <= 0) {
                power = type.power(fuel);
            }
            if (starting) {
                duration = fuelDuration;
            } else if (duration <= 0) {
                duration = Math.max((int) Math.ceil(time), Math.max(type.defaultDuration(), fuelDuration));
            }

            IEnergyStorage storage = generator.getCapability(ForgeCapabilities.ENERGY).orElseThrow(IllegalStateException::new);
            speed = Math.min(speed, storage.getMaxEnergyStored() / (double) power);
            long fuelEnergy = Math.round((starting ? duration : time) * power);
            int amount = (int) Math.min(fuelEnergy, Math.round(power * speed));
            if (amount > 0 && storage.receiveEnergy(amount, true) == amount) {
                if (starting) {
                    generator.consumeFuel();
                    fuelDuration = type.burnDuration(generator.getFuelStack());
                }
                time = (fuelEnergy - amount) / (double) power;
                data.putDouble("time", time);
                data.putInt("burnDuration", duration);
                data.putInt("burnPower", power);
                generated = storage.receiveEnergy(amount, false);
                UpdateRGBProcedure.execute(level, generator, type.rainbowKey());
            }
        }

        GeneratorOutput.syncBurnProgress(level, generator, fuelDuration > 0 || time > 0, time, duration);
        GeneratorOutput.finishTick(level, pos, generated, speed);
    }
}
