package inzhefop.extrautilitiesrebirth.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

public abstract class DrumBlockEntity extends BlockEntity {
    public static final String FLUID_TAG = "fluidTank";

    private final boolean creative;
    private final FluidTank tank;
    private LazyOptional<IFluidHandler> fluid;

    protected DrumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int capacity, boolean creative) {
        super(type, pos, state);
        this.creative = creative;
        this.tank = new FluidTank(capacity) {
            @Override
            public boolean isFluidValid(FluidStack stack) {
                return !creative && super.isFluidValid(stack);
            }

            @Override
            public FluidStack getFluidInTank(int index) {
                if (index != 0) {
                    return FluidStack.EMPTY;
                }
                return creative && !isEmpty() ? new FluidStack(getFluid(), getCapacity()) : getFluid();
            }

            @Override
            public FluidStack drain(int amount, FluidAction action) {
                if (amount <= 0 || isEmpty()) {
                    return FluidStack.EMPTY;
                }
                return creative ? new FluidStack(getFluid(), amount) : super.drain(amount, action);
            }

            @Override
            protected void onContentsChanged() {
                syncContents();
            }
        };
        fluid = LazyOptional.of(() -> tank);
    }

    public boolean isCreativeDrum() {
        return creative;
    }

    public boolean configure(FluidStack sample) {
        if (!creative || !tank.isEmpty() || sample.isEmpty()) {
            return false;
        }
        tank.setFluid(new FluidStack(sample, 1));
        syncContents();
        return true;
    }

    public int comparatorSignal() {
        int amount = tank.getFluidInTank(0).getAmount();
        return amount == 0 ? 0 : 1 + (int) (14L * amount / tank.getCapacity());
    }

    private void syncContents() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(FLUID_TAG, Tag.TAG_COMPOUND)) {
            FluidStack stored = FluidStack.loadFluidStackFromNBT(tag.getCompound(FLUID_TAG));
            tank.setFluid(stored.isEmpty() ? FluidStack.EMPTY : new FluidStack(stored, Math.min(stored.getAmount(), tank.getCapacity())));
        } else {
            tank.setFluid(FluidStack.EMPTY);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put(FLUID_TAG, tank.writeToNBT(new CompoundTag()));
    }

    public CompoundTag saveCarriedFluid() {
        CompoundTag tag = new CompoundTag();
        if (!tank.isEmpty()) {
            tag.put(FLUID_TAG, tank.writeToNBT(new CompoundTag()));
        }
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (!isRemoved() && capability == ForgeCapabilities.FLUID_HANDLER) {
            return fluid.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        fluid.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        fluid = LazyOptional.of(() -> tank);
    }
}
