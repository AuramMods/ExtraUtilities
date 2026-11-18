package inzhefop.extrautilitiesrebirth.block.entity;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.items.IItemHandler;

import net.minecraftforge.energy.EnergyStorage;

import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.Capability;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.stream.IntStream;

import io.netty.buffer.Unpooled;

import inzhefop.extrautilitiesrebirth.world.inventory.MechanicalMinerGUIMenu;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;

public class MechanicalMinerBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	public static final int UPGRADE_SLOT = 9;
	private final Deque<ItemStack> pendingDrops = new ArrayDeque<>();
	private NonNullList<ItemStack> stacks = NonNullList.withSize(UPGRADE_SLOT + 1, ItemStack.EMPTY);
	private final LazyOptional<? extends IItemHandler>[] handlers = SidedInvWrapper.create(this, Direction.values());

	public MechanicalMinerBlockEntity(BlockPos position, BlockState state) {
		super(ExtrautilitiesrebirthModBlockEntities.MECHANICAL_MINER.get(), position, state);
	} 

	@Override
	public void load(CompoundTag compound) {
		super.load(compound);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks);
		pendingDrops.clear();
		ListTag savedDrops = compound.getList("PendingDrops", Tag.TAG_COMPOUND);
		for (int index = 0; index < savedDrops.size(); index++) {
			ItemStack stack = ItemStack.of(savedDrops.getCompound(index));
			if (!stack.isEmpty()) {
				pendingDrops.addLast(stack);
			}
		}
		if (compound.get("energyStorage") instanceof IntTag intTag)
			energyStorage.deserializeNBT(intTag);
	}

	@Override
	public void saveAdditional(CompoundTag compound) {
		super.saveAdditional(compound);
		if (!this.trySaveLootTable(compound)) {
			ContainerHelper.saveAllItems(compound, this.stacks);
		}
		compound.put("energyStorage", energyStorage.serializeNBT());
		if (!pendingDrops.isEmpty()) {
			ListTag savedDrops = new ListTag();
			for (ItemStack stack : pendingDrops) {
				savedDrops.add(stack.save(new CompoundTag()));
			}
			compound.put("PendingDrops", savedDrops);
		}
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
		return Component.literal("mechanical_miner");
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new MechanicalMinerGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Mechanical Miner");
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
		return index == UPGRADE_SLOT && MachineSpeed.factorOf(stack) > 0;
	}

	public double miningSpeed() {
		return MachineSpeed.multiplier(getItem(UPGRADE_SLOT));
	}

	public boolean hasOutputSpace() {
		for (int slot = 0; slot < UPGRADE_SLOT; slot++) {
			ItemStack stored = getItem(slot);
			if (stored.isEmpty() || stored.getCount() < Math.min(stored.getMaxStackSize(), getMaxStackSize())) {
				return true;
			}
		}
		return false;
	}

	public void addMinedDrops(List<ItemStack> drops) {
		for (ItemStack drop : drops) {
			ItemStack remaining = drop.copy();
			while (!remaining.isEmpty()) {
				pendingDrops.addLast(remaining.split(Math.min(remaining.getMaxStackSize(), getMaxStackSize())));
			}
		}
		setChanged();
	}

	public boolean flushPendingDrops() {
		while (!pendingDrops.isEmpty()) {
			ItemStack drop = pendingDrops.getFirst();
			for (int slot = 0; slot < UPGRADE_SLOT && !drop.isEmpty(); slot++) {
				ItemStack stored = getItem(slot);
				if (!stored.isEmpty() && ItemStack.isSameItemSameTags(stored, drop)) {
					int capacity = Math.min(stored.getMaxStackSize(), getMaxStackSize()) - stored.getCount();
					if (capacity > 0) {
						int moved = Math.min(capacity, drop.getCount());
						drop.shrink(moved);
						setItem(slot, stored.copyWithCount(stored.getCount() + moved));
					}
				}
			}
			for (int slot = 0; slot < UPGRADE_SLOT && !drop.isEmpty(); slot++) {
				if (getItem(slot).isEmpty()) {
					setItem(slot, drop.split(Math.min(drop.getMaxStackSize(), getMaxStackSize())));
				}
			}
			if (!drop.isEmpty()) {
				return false;
			}
			pendingDrops.removeFirst();
		}
		return true;
	}

	public void dropPendingOutput() {
		if (level != null && !level.isClientSide) {
			while (!pendingDrops.isEmpty()) {
				Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
						pendingDrops.removeFirst());
			}
		}
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
		return index >= 0 && index < UPGRADE_SLOT;
	}

	private final EnergyStorage energyStorage = new EnergyStorage(25000, 25000, 25000, 0) {
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

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
		if (!this.remove && facing != null && capability == ForgeCapabilities.ITEM_HANDLER)
			return handlers[facing.ordinal()].cast();
		if (!this.remove && capability == ForgeCapabilities.ENERGY)
			return LazyOptional.of(() -> energyStorage).cast();
		return super.getCapability(capability, facing);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		for (LazyOptional<? extends IItemHandler> handler : handlers)
			handler.invalidate();
	}
}
