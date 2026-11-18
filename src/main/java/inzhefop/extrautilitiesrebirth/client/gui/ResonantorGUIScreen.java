package inzhefop.extrautilitiesrebirth.client.gui;

import inzhefop.extrautilitiesrebirth.world.inventory.ResonantorGUIMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ResonantorGUIScreen extends PoweredProcessorScreen<ResonantorGUIMenu> {
    public ResonantorGUIScreen(ResonantorGUIMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "resonantor_gui.png", 19, 68, 34, false);
    }
}
