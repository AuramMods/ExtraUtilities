package inzhefop.extrautilitiesrebirth.world.inventory;

import inzhefop.extrautilitiesrebirth.block.entity.QuantumQuarryBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class EnderQuarryGUIMenu extends AbstractContainerMenu {
	private static final int QUARRY_SLOTS = 4;
	private final QuantumQuarryBlockEntity quarry;

	public EnderQuarryGUIMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
		this(id, inventory, findQuarry(inventory, extraData.readBlockPos()));
	}

	public EnderQuarryGUIMenu(int id, Inventory inventory, QuantumQuarryBlockEntity quarry) {
		super(ExtrautilitiesrebirthModMenus.ENDER_QUARRY_GUI.get(), id);
		this.quarry = quarry;
		Container contents = inventory.player.level().isClientSide || quarry == null
				? new SimpleContainer(QUARRY_SLOTS) : quarry;
		for (int i = 0; i < QuantumQuarryBlockEntity.UPGRADE_SLOT; i++) {
			addSlot(new Slot(contents, i, 62 + i * 18, 38) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}
			});
		}
		addSlot(new Slot(contents, QuantumQuarryBlockEntity.UPGRADE_SLOT, 126, 38) {
			@Override
			public boolean mayPlace(ItemStack stack) {
				return QuantumQuarryBlockEntity.isMiningBook(stack);
			}

			@Override
			public int getMaxStackSize() {
				return 1;
			}
		});

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				addSlot(new Slot(inventory, column + (row + 1) * 9, 8 + column * 18, 84 + row * 18));
			}
		}
		for (int column = 0; column < 9; column++) {
			addSlot(new Slot(inventory, column, 8 + column * 18, 142));
		}
	}

	private static QuantumQuarryBlockEntity findQuarry(Inventory inventory, BlockPos pos) {
		return inventory.player.level().getBlockEntity(pos) instanceof QuantumQuarryBlockEntity quarry ? quarry : null;
	}

	public QuantumQuarryBlockEntity getQuarry() {
		return quarry;
	}

	@Override
	public boolean stillValid(Player player) {
		return player.level().isClientSide || quarry != null && quarry.stillValid(player) && quarry.isAssembled();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack original = stack.copy();
		if (index < QUARRY_SLOTS) {
			if (!moveItemStackTo(stack, QUARRY_SLOTS, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else if (!moveItemStackTo(stack, QuantumQuarryBlockEntity.UPGRADE_SLOT, QUARRY_SLOTS, false)) {
			int hotbarStart = QUARRY_SLOTS + 27;
			if (index < hotbarStart ? !moveItemStackTo(stack, hotbarStart, slots.size(), false)
					: !moveItemStackTo(stack, QUARRY_SLOTS, hotbarStart, false)) {
				return ItemStack.EMPTY;
			}
		}
		if (stack.isEmpty()) {
			slot.set(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		slot.onTake(player, stack);
		return original;
	}
}
