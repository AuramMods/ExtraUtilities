package inzhefop.extrautilitiesrebirth.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import inzhefop.extrautilitiesrebirth.world.inventory.PoweredProcessorMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

abstract class PoweredProcessorScreen<T extends PoweredProcessorMenu> extends AbstractContainerScreen<T> {
    private static final ResourceLocation ENERGY_HOLDER = texture("energy_bar_holder.png");
    private static final ResourceLocation ENERGY_FILL = texture("energybarfill.png");
    private static final ResourceLocation ENERGY_EMPTY = texture("gray.png");
    private static final ResourceLocation ARROW = texture("arrow.png");
    private static final ResourceLocation COMPACT_ARROW = texture("progress_arrow_back.png");
    private static final ResourceLocation ARROW_FILL = texture("progress_arrow_fill.png");
    private static final ResourceLocation ARROW_OVERLAY = texture("progress_arrow_negative.png");
    private static final ResourceLocation UPGRADE_ICON = texture("upgrade_icon.png");

    private final ResourceLocation background;
    private final int upgradeY;
    private final int arrowX;
    private final int arrowY;
    private final boolean compactArrow;

    protected PoweredProcessorScreen(T menu, Inventory inventory, Component title, String background,
                                     int upgradeY, int arrowX, int arrowY, boolean compactArrow) {
        super(menu, inventory, title);
        this.background = texture("screens/" + background);
        this.upgradeY = upgradeY;
        this.arrowX = arrowX;
        this.arrowY = arrowY;
        this.compactArrow = compactArrow;
        titleLabelX = 7;
        titleLabelY = 5;
        inventoryLabelX = 7;
        inventoryLabelY = 72;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        graphics.blit(background, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
        graphics.blit(UPGRADE_ICON, leftPos + 8, topPos + upgradeY, 0, 0, 16, 16, 16, 16);
        graphics.blit(ENERGY_HOLDER, leftPos + 151, topPos + 7, 0, 0, 18, 72, 18, 72);
        graphics.blit(ENERGY_FILL, leftPos + 152, topPos + 8, 0, 0, 16, 70, 16, 70);
        int empty = 70 - menu.energyPixels();
        if (empty > 0) {
            graphics.blit(ENERGY_EMPTY, leftPos + 152, topPos + 8, 0, 0, 16, empty, 16, 70);
        }

        int x = leftPos + arrowX;
        int y = topPos + arrowY;
        if (compactArrow) {
            graphics.blit(COMPACT_ARROW, x, y, 0, 0, 22, 15, 22, 15);
        } else {
            graphics.blit(ARROW, x - 6, y - 1, 0, 0, 32, 18, 32, 18);
        }
        int progress = menu.progressPixels();
        if (progress > 0) {
            graphics.blit(ARROW_FILL, x, y, 0, 0, progress, 15, 22, 15);
        }
        graphics.blit(ARROW_OVERLAY, x, y, 0, 0, 22, 15, 22, 15);

        RenderSystem.disableBlend();
    }

    private static ResourceLocation texture(String path) {
        return ResourceLocation.fromNamespaceAndPath("extrautilitiesrebirth", "textures/" + path);
    }
}
