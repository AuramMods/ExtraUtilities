package inzhefop.extrautilitiesrebirth.client.gui;

import inzhefop.extrautilitiesrebirth.world.inventory.FurnaceGUIMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class FurnaceGUIScreen extends PoweredProcessorScreen<FurnaceGUIMenu> {
    public FurnaceGUIScreen(FurnaceGUIMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "furnace_gui.png", 18, 69, 33, false);
    }
}
