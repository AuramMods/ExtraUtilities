package inzhefop.extrautilitiesrebirth.procedures;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

final class MechanicalWork {
    private MechanicalWork() {
    }

    static boolean advance(BlockEntity machine, double speed) {
        int power = (int) Math.pow(4 * speed, 2);
        var energy = machine.getCapability(ForgeCapabilities.ENERGY).orElseThrow(IllegalStateException::new);
        if (energy.extractEnergy(power, true) != power) {
            return false;
        }
        energy.extractEnergy(power, false);
        int ticks = Math.max(0, machine.getPersistentData().getInt("ticks")) + 1;
        if (ticks < 100 / speed) {
            setProgress(machine, ticks);
            return false;
        }
        reset(machine);
        return true;
    }

    static void reset(BlockEntity machine) {
        setProgress(machine, 0);
    }

    private static void setProgress(BlockEntity machine, int ticks) {
        if (machine.getPersistentData().getInt("ticks") != ticks) {
            machine.getPersistentData().putDouble("ticks", ticks);
            machine.setChanged();
        }
    }
}
