package inzhefop.extrautilitiesrebirth.block.entity;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.items.IItemHandler;

import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.Capability;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

import java.util.Locale;
import java.util.stream.IntStream;

import io.netty.buffer.Unpooled;

import inzhefop.extrautilitiesrebirth.world.inventory.SorterGUIMenu;
import inzhefop.extrautilitiesrebirth.block.SortingMachineBlock;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;

public class SortingMachineBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
	public static final int UPGRADE_SLOT = 15;
	public static final int FILTER_SLOT = 16;
	private NonNullList<ItemStack> stacks = NonNullList.withSize(FILTER_SLOT + 1, ItemStack.EMPTY);
	private final LazyOptional<? extends IItemHandler>[] handlers = SidedInvWrapper.create(this, Direction.values());

	public SortingMachineBlockEntity(BlockPos position, BlockState state) {
		super(ExtrautilitiesrebirthModBlockEntities.SORTING_MACHINE.get(), position, state);
	} 

	@Override
	public void load(CompoundTag compound) {
		super.load(compound);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks);
	}

	@Override
	public void saveAdditional(CompoundTag compound) {
		super.saveAdditional(compound);
		if (!this.trySaveLootTable(compound)) {
			ContainerHelper.saveAllItems(compound, this.stacks);
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
		return Component.literal("sorting_machine");
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return new SorterGUIMenu(id, inventory, new FriendlyByteBuf(Unpooled.buffer()).writeBlockPos(this.worldPosition));
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Sorting Machine");
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
		if (index == UPGRADE_SLOT) {
			return MachineSpeed.factorOf(stack) > 0;
		}
		if (index == FILTER_SLOT) {
			return stack.is(Items.NAME_TAG);
		}
		return index >= 0 && index < UPGRADE_SLOT;
	}

	public int ticksPerTransfer() {
		ItemStack upgrade = getItem(UPGRADE_SLOT);
		int reduction = MachineSpeed.factorOf(upgrade) > 0 ? upgrade.getCount() : 0;
		return Math.max(1, 20 - reduction);
	}

	@Nullable
	public TagKey<Item> filter() {
		ItemStack stack = getItem(FILTER_SLOT);
		if (level == null || !stack.is(Items.NAME_TAG) || !stack.hasCustomHoverName()) {
			return null;
		}
		String text = stack.getHoverName().getString().trim().toLowerCase(Locale.ROOT);
		ResourceLocation id = ResourceLocation.tryParse(text);
		if (id == null) {
			return null;
		}
		TagKey<Item> tag = ItemTags.create(id);
		return level.registryAccess().registryOrThrow(Registries.ITEM).getTag(tag).isPresent() ? tag : null;
	}

	public LazyOptional<IItemHandler> output(boolean matchesFilter) {
		if (level == null) {
			return LazyOptional.empty();
		}
		Direction facing = getBlockState().getValue(SortingMachineBlock.FACING);
		Direction side = matchesFilter ? facing.getCounterClockWise() : facing.getClockWise();
		BlockPos destination = worldPosition.relative(side);
		BlockEntity neighbor = level.hasChunkAt(destination) ? level.getBlockEntity(destination) : null;
		return neighbor == null ? LazyOptional.empty() : neighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, side.getOpposite());
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
		return false;
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction facing) {
		if (!this.remove && facing != null && capability == ForgeCapabilities.ITEM_HANDLER)
			return handlers[facing.ordinal()].cast();
		return super.getCapability(capability, facing);
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		for (LazyOptional<? extends IItemHandler> handler : handlers)
			handler.invalidate();
	}
}
