package inzhefop.extrautilitiesrebirth.world.inventory;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModMenus;
import inzhefop.extrautilitiesrebirth.item.inventory.GoldenBagOfHoldingInventoryCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class GoldenBagGUIMenu extends StorageMenu {
    private final Inventory inventory;
    private final int bagSlot;
    private final ItemStack bag;

    public GoldenBagGUIMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, readHand(extraData));
    }

    public GoldenBagGUIMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ExtrautilitiesrebirthModMenus.GOLDEN_BAG_GUI.get(), id);
        this.inventory = inventory;
        bagSlot = hand == InteractionHand.MAIN_HAND ? inventory.selected : Inventory.SLOT_OFFHAND;
        bag = inventory.getItem(bagSlot);
        IItemHandler storage = inventory.player.level().isClientSide
                ? new GoldenBagOfHoldingInventoryCapability().getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new)
                : bag.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(IllegalStateException::new);
        for (int index = 0; index < STORAGE_SLOTS; index++) {
            addSlot(new SlotItemHandler(storage, index, 8 + index % 9 * 18, 20 + index / 9 * 18));
        }
        addPlayerSlots(inventory, bagSlot);
    }

    private static InteractionHand readHand(FriendlyByteBuf extraData) {
        extraData.readBlockPos();
        return extraData.readByte() == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == inventory.player && bag.is(ExtrautilitiesrebirthModItems.GOLDEN_BAG_OF_HOLDING.get())
                && inventory.getItem(bagSlot) == bag;
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (type != ClickType.SWAP || button != bagSlot) {
            super.clicked(slot, button, type, player);
        }
    }
}
