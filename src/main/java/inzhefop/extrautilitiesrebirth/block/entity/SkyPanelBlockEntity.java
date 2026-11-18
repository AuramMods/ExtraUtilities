package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.procedures.GeneratorOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public abstract class SkyPanelBlockEntity extends BlockEntity {
    private static final int CAPACITY = 100_000;

    private final boolean generatesInDaylight;
    private final int energyPerTick;
    private final PanelEnergyStorage storage = new PanelEnergyStorage();
    private LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> storage);

    protected SkyPanelBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean generatesInDaylight, int energyPerTick) {
        super(type, pos, state);
        this.generatesInDaylight = generatesInDaylight;
        this.energyPerTick = energyPerTick;
    }

    public void tickEnergy(ServerLevel level) {
        if (level.dimensionType().hasSkyLight() && level.isDay() == generatesInDaylight
                && level.canSeeSkyFromBelowWater(worldPosition)) {
            storage.generate(energyPerTick);
        }
        GeneratorOutput.transferStoredEnergy(level, this, CAPACITY);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        storage.deserializeNBT(IntTag.valueOf(Mth.clamp(tag.getInt("energyStorage"), 0, CAPACITY)));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("energyStorage", storage.serializeNBT());
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

    private final class PanelEnergyStorage extends EnergyStorage {
        private PanelEnergyStorage() {
            super(CAPACITY, 0, CAPACITY);
        }

        private void generate(int amount) {
            int next = Math.min(capacity, energy + amount);
            if (next != energy) {
                energy = next;
                setChanged();
            }
        }

        @Override
        public int extractEnergy(int amount, boolean simulate) {
            int extracted = super.extractEnergy(Math.max(0, amount), simulate);
            if (extracted > 0 && !simulate) {
                setChanged();
            }
            return extracted;
        }
    }
}
