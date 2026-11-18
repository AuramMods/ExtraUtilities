package inzhefop.extrautilitiesrebirth.block.entity;

import net.minecraftforge.energy.IEnergyStorage;

record GeneratorEnergyOutput(IEnergyStorage storage) implements IEnergyStorage {
    @Override
    public int receiveEnergy(int amount, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int amount, boolean simulate) {
        return storage.extractEnergy(amount, simulate);
    }

    @Override
    public int getEnergyStored() {
        return storage.getEnergyStored();
    }

    @Override
    public int getMaxEnergyStored() {
        return storage.getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return storage.canExtract();
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}
