package inzhefop.extrautilitiesrebirth.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import inzhefop.extrautilitiesrebirth.world.inventory.SorterGUIMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class SorterGUIScreen extends AbstractContainerScreen<SorterGUIMenu> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("extrautilitiesrebirth:textures/screens/sorter_gui.png");
    private static final ResourceLocation UPGRADE_ICON = new ResourceLocation("extrautilitiesrebirth:textures/upgrade_icon.png");
    private static final ResourceLocation FILTER_ICON = new ResourceLocation("extrautilitiesrebirth:textures/screens/name_tag.png");
    private static final ResourceLocation MISSING_OUTPUT = new ResourceLocation("extrautilitiesrebirth:textures/screens/normal.png");
    private static final ResourceLocation INVALID_FILTER = new ResourceLocation("extrautilitiesrebirth:textures/screens/attention_sign.png");

    public SorterGUIScreen(SorterGUIMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        graphics.blit(UPGRADE_ICON, leftPos + 8, topPos + 23, 0, 0, 16, 16, 16, 16);
        graphics.blit(FILTER_ICON, leftPos + 8, topPos + 42, 0, 0, 16, 16, 16, 16);
        if (!menu.hasOtherOutput()) {
            graphics.blit(MISSING_OUTPUT, leftPos + 144, topPos + 24, 0, 0, 18, 14, 18, 14);
        }
        if (!menu.hasMatchingOutput()) {
            graphics.blit(MISSING_OUTPUT, leftPos + 144, topPos + 42, 0, 0, 18, 14, 18, 14);
        }
        if (!menu.hasValidFilter()) {
            graphics.blit(INVALID_FILTER, leftPos + 27, topPos + 43, 0, 0, 4, 14, 4, 14);
        }
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        String label = menu.filterLabel();
        String text = label.isEmpty() ? "Sorter" : "Sorter [" + label + "]";
        graphics.drawString(font, font.plainSubstrByWidth(text, imageWidth - 12), 6, 7, 0x404040, false);
    }
}
