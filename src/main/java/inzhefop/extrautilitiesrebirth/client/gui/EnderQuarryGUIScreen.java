package inzhefop.extrautilitiesrebirth.client.gui;

import inzhefop.extrautilitiesrebirth.world.inventory.EnderQuarryGUIMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public class EnderQuarryGUIScreen extends AbstractContainerScreen<EnderQuarryGUIMenu> {
	private static final ResourceLocation TEXTURE = new ResourceLocation("extrautilitiesrebirth:textures/screens/ender_quarry_gui.png");

	public EnderQuarryGUIScreen(EnderQuarryGUIMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.titleLabelX = 6;
		this.titleLabelY = 5;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTicks);
		renderTooltip(graphics, mouseX, mouseY);
		if (menu.getSlot(3).getItem().isEmpty() && isHovering(126, 38, 16, 16, mouseX, mouseY)) {
			graphics.renderTooltip(font, Component.translatable("gui.extrautilitiesrebirth.quantum_quarry.upgrade"), mouseX, mouseY);
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
		graphics.blit(TEXTURE, leftPos + 125, topPos + 37, 61, 37, 18, 18, imageWidth, imageHeight);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderLabels(graphics, mouseX, mouseY);
		var quarry = menu.getQuarry();
		if (quarry != null) {
			quarry.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> graphics.drawString(font,
					Component.translatable("gui.extrautilitiesrebirth.generator.stored", energy.getEnergyStored(),
							energy.getMaxEnergyStored()), 6, 57, 4210752, false));
		}
	}
}
