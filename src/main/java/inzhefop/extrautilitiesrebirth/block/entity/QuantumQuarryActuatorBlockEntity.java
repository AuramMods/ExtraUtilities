package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.block.QuantumQuarryActuatorBlock;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public class QuantumQuarryActuatorBlockEntity extends BlockEntity {
    private LazyOptional<IEnergyStorage> input = LazyOptional.of(ActuatorInput::new);

    public QuantumQuarryActuatorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.QUANTUM_QUARRY_ACTUATOR.get(), pos, state);
    }

    private IEnergyStorage linkedBuffer() {
        if (level != null && !isRemoved()) {
            QuantumQuarryBlockEntity quarry = QuantumQuarryActuatorBlock.findQuarry(level, worldPosition, getBlockState());
            if (quarry != null && quarry.isAssembled()) {
                return quarry.getCapability(ForgeCapabilities.ENERGY).orElse(null);
            }
        }
        return null;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction face) {
        if (capability == ForgeCapabilities.ENERGY && !isRemoved()) {
            return input.cast();
        }
        return super.getCapability(capability, face);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        input.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        input = LazyOptional.of(ActuatorInput::new);
    }

    private class ActuatorInput implements IEnergyStorage {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            IEnergyStorage buffer = linkedBuffer();
            return buffer == null ? 0 : buffer.receiveEnergy(amount, simulate);
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            IEnergyStorage buffer = linkedBuffer();
            return buffer == null ? 0 : buffer.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            IEnergyStorage buffer = linkedBuffer();
            return buffer == null ? 0 : buffer.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return linkedBuffer() != null;
        }
    }
}
