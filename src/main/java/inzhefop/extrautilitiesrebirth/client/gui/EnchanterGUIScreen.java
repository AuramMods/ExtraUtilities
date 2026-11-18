package inzhefop.extrautilitiesrebirth.client.gui;

import inzhefop.extrautilitiesrebirth.world.inventory.EnchanterGUIMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class EnchanterGUIScreen extends PoweredProcessorScreen<EnchanterGUIMenu> {
    public EnchanterGUIScreen(EnchanterGUIMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "enchanter_gui.png", 19, 78, 35, true);
    }
}
