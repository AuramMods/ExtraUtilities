package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

public class EnderThermicPumpBlockEntity extends BlockEntity {
    private final EnergyStorage energyStorage = new EnergyStorage(10_000_000) {
        @Override
        public int receiveEnergy(int amount, boolean simulate) {
            int received = super.receiveEnergy(amount, simulate);
            if (!simulate && received > 0) {
                setChanged();
            }
            return received;
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int extracted = super.extractEnergy(amount, simulate);
            if (!simulate && extracted > 0) {
                setChanged();
            }
            return extracted;
        }
    };

    private final FluidTank fluidTank = new FluidTank(64_000, stack ->
            stack.getFluid().isSame(Fluids.WATER) || stack.getFluid().isSame(Fluids.LAVA)) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private LazyOptional<EnergyStorage> energy = LazyOptional.of(() -> energyStorage);
    private LazyOptional<FluidTank> fluid = LazyOptional.of(() -> fluidTank);

    public EnderThermicPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.ENDER_THERMIC_PUMP.get(), pos, state);
    }

    public FluidTank tank() {
        return fluidTank;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.deserializeNBT(tag.get("energyStorage") instanceof IntTag savedEnergy ? savedEnergy : IntTag.valueOf(0));
        if (tag.contains("fluidTank", Tag.TAG_COMPOUND)) {
            fluidTank.readFromNBT(tag.getCompound("fluidTank"));
        } else {
            fluidTank.setFluid(FluidStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("energyStorage", energyStorage.serializeNBT());
        tag.put("fluidTank", fluidTank.writeToNBT(new CompoundTag()));
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithFullMetadata();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (!isRemoved()) {
            if (capability == ForgeCapabilities.ENERGY) {
                return energy.cast();
            }
            if (capability == ForgeCapabilities.FLUID_HANDLER) {
                return fluid.cast();
            }
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energy.invalidate();
        fluid.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        energy = LazyOptional.of(() -> energyStorage);
        fluid = LazyOptional.of(() -> fluidTank);
    }
}
