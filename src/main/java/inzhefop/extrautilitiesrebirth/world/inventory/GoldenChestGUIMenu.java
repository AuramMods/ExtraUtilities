package inzhefop.extrautilitiesrebirth.world.inventory;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

public class GoldenChestGUIMenu extends StorageMenu {
    private final Container chest;

    public GoldenChestGUIMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, new SimpleContainer(STORAGE_SLOTS));
    }

    public GoldenChestGUIMenu(int id, Inventory inventory, Container chest) {
        super(ExtrautilitiesrebirthModMenus.GOLDEN_CHEST_GUI.get(), id);
        this.chest = chest;
        for (int index = 0; index < STORAGE_SLOTS; index++) {
            addSlot(new Slot(chest, index, 8 + index % 9 * 18, 20 + index / 9 * 18));
        }
        addPlayerSlots(inventory, -1);
    }

    @Override
    public boolean stillValid(Player player) {
        return chest.stillValid(player);
    }
}
