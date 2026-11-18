package inzhefop.extrautilitiesrebirth.item.inventory;

import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.ItemStackHandler;

import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.Capability;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;

import javax.annotation.Nullable;
import javax.annotation.Nonnull;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;

public class GoldenBagOfHoldingInventoryCapability implements ICapabilitySerializable<CompoundTag> {
	private final LazyOptional<ItemStackHandler> inventory = LazyOptional.of(this::createItemHandler);

	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side) {
		return capability == ForgeCapabilities.ITEM_HANDLER ? this.inventory.cast() : LazyOptional.empty();
	}

	@Override
	public CompoundTag serializeNBT() {
		return getItemHandler().serializeNBT();
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		getItemHandler().deserializeNBT(nbt);
	}

	private ItemStackHandler createItemHandler() {
		return new ItemStackHandler(54) {
			@Override
			public int getSlotLimit(int slot) {
				return 64;
			}

			@Override
			public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
				return stack.getItem() != ExtrautilitiesrebirthModItems.GOLDEN_BAG_OF_HOLDING.get();
			}

			@Override
			public void setSize(int size) {
			}
		};
	}

	private ItemStackHandler getItemHandler() {
		return inventory.orElseThrow(RuntimeException::new);
	}
}
