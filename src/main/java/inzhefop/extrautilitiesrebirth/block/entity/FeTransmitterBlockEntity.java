package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class FeTransmitterBlockEntity extends BlockEntity {
    private static final int CAPACITY = 3_200;
    private static final String ENERGY_TAG = "energyStorage";

    private final EnergyStorage storage = new EnergyStorage(CAPACITY) {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(Math.max(0, amount), simulate);
            if (!simulate && received > 0) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int extracted = super.extractEnergy(Math.max(0, amount), simulate);
            if (!simulate && extracted > 0) {
                setChanged();
            }
            return extracted;
        }
    };
    private LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> storage);

    public FeTransmitterBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.FE_TRANSMITTER.get(), pos, state);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storage.deserializeNBT(IntTag.valueOf(Mth.clamp(tag.getInt(ENERGY_TAG), 0, CAPACITY)));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(ENERGY_TAG, storage.serializeNBT());
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (!isRemoved() && capability == ForgeCapabilities.ENERGY) {
            return energy.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energy.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energy = LazyOptional.of(() -> storage);
    }
}
