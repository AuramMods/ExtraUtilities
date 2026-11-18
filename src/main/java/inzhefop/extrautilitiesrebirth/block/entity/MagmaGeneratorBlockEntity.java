package inzhefop.extrautilitiesrebirth.block.entity;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.items.IItemHandler;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;

import net.minecraftforge.energy.EnergyStorage;

import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.Capability;

import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

import java.util.stream.IntStream;

import io.netty.buffer.Unpooled;

import inzhefop.extrautilitiesrebirth.world.inventory.NormalGUIMenu;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;

public class MagmaGeneratorBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	private NonNullList<ItemStack> stacks = NonNullList.<ItemStack>withSize(2, ItemStack.EMPTY);
	private final LazyOptional<? extends IItemHandler>[] handlers = SidedInvWrapper.create(this, Direction.values());

	public MagmaGeneratorBlockEntity(BlockPos position, BlockState state) {
		super(ExtrautilitiesrebirthModBlockEntities.MAGMA_GENERATOR.get(), position, state);
	}
 
	@Override
	public void load(CompoundTag compound) {
		super.load(compound);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks);
		if (compound.get("energyStorage") instanceof IntTag intTag)
			energyStorage.deserializeNBT(intTag);
		if (compound.get("fluidTank") instanceof CompoundTag compoundTag)
			fluidTank.readFromNBT(compoundTag);
	}

	@Override
	public void saveAdditional(CompoundTag compound) {
		super.saveAdditional(compound);
		if (!this.trySaveLootTable(compound)) {
			ContainerHelper.saveAllItems(compound, this.stacks);
		}
		compound.put("energyStorage", energyStorage.serializeNBT());
		compound.put("fluidTank", fluidTank.writeToNBT(new CompoundTag()));
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag() {
		return this.saveWithFullMetadata();
	}

	@Override
	public int getContainerSize() {
		return stacks.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.stacks)
			if (!itemstack.isEmpty())
				return false;
		return true;
	}

	@Override
	public Component getDefaultName() {
		return Component.literal("magma_generator");
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new NormalGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Magma Generator");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return index == 1 && upgradeSpeed(stack) > 1;
	}

	public double getSpeedMultiplier() {
		return upgradeSpeed(getItem(1));
	}

	private static double upgradeSpeed(ItemStack stack) {
		if (stack.is(ExtrautilitiesrebirthModItems.SPEED_UPGRADE.get())) {
			return stack.getCount() * 1.05;
		}
		if (stack.is(ExtrautilitiesrebirthModItems.ENCHANTED_SPEED_UPGRADE.get())) {
			return stack.getCount() * 1.1;
		}
		if (stack.is(ExtrautilitiesrebirthModItems.SUPER_SPEED_UPGRADE.get())) {
			return stack.getCount() * 1.2;
		}
		return 1;
	}

	public boolean hasSpaceForEnergy(int energy) {
		return energyStorage.receiveEnergy(energy, true) == energy;
	}

	public boolean hasLavaBucket() {
		return fluidTank.getFluidAmount() >= FluidType.BUCKET_VOLUME;
	}

	public void consumeLavaBucket() {
		fluidTank.drain(FluidType.BUCKET_VOLUME, FluidAction.EXECUTE);
	}

	public boolean fillLavaBucket() {
		FluidStack lava = new FluidStack(Fluids.LAVA, FluidType.BUCKET_VOLUME);
		if (fluidTank.fill(lava, FluidAction.SIMULATE) != FluidType.BUCKET_VOLUME) {
			return false;
		}
		fluidTank.fill(lava, FluidAction.EXECUTE);
		return true;
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return IntStream.range(0, this.getContainerSize()).toArray();
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
		return this.canPlaceItem(index, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
		if (index == 0)
			return false;
		if (index == 1)
			return false;
		return true;
	}

	private final EnergyStorage energyStorage = new EnergyStorage(100000, 100000, 100000, 0) {
		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			int retval = super.receiveEnergy(maxReceive, simulate);
			if (!simulate) {
				setChanged();
				level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
			}
			return retval;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			int retval = super.extractEnergy(maxExtract, simulate);
			if (!simulate) {
				setChanged();
				level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
			}
			return retval;
		}
	};
	private final FluidTank fluidTank = new FluidTank(8000, fs -> {
		if (fs.getFluid() == Fluids.FLOWING_LAVA)
			return true;
		if (fs.getFluid() == Fluids.LAVA)
			return true;
		return false;
	}) {
		@Override
		protected void onContentsChanged() {
			super.onContentsChanged();
			setChanged();
			level.sendBlockUpdated(worldPosition, level.getBlockState(worldPosition), level.getBlockState(worldPosition), 2);
		}
	};

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
		if (!this.remove && facing != null && capability == ForgeCapabilities.ITEM_HANDLER)
			return handlers[facing.ordinal()].cast();
		if (!this.remove && capability == ForgeCapabilities.ENERGY)
			return LazyOptional.of(() -> energyStorage).cast();
		if (!this.remove && capability == ForgeCapabilities.FLUID_HANDLER)
			return LazyOptional.of(() -> fluidTank).cast();
		return super.getCapability(capability, facing);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		for (LazyOptional<? extends IItemHandler> handler : handlers)
			handler.invalidate();
	}
}
