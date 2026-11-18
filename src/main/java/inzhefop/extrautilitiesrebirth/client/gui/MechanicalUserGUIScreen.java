package inzhefop.extrautilitiesrebirth.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import inzhefop.extrautilitiesrebirth.world.inventory.MechanicalUserGUIMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;

public class MechanicalUserGUIScreen extends AbstractContainerScreen<MechanicalUserGUIMenu> {
    private static final ResourceLocation BACKGROUND = new ResourceLocation("extrautilitiesrebirth:textures/screens/mechanical_user_gui.png");
    private static final ResourceLocation UPGRADE_ICON = new ResourceLocation("extrautilitiesrebirth:textures/upgrade_icon.png");
    private static final ResourceLocation ENERGY_HOLDER = new ResourceLocation("extrautilitiesrebirth:textures/energy_bar_holder.png");
    private static final ResourceLocation ENERGY_FILL = new ResourceLocation("extrautilitiesrebirth:textures/energybarfill.png");
    private static final ResourceLocation ENERGY_EMPTY = new ResourceLocation("extrautilitiesrebirth:textures/gray.png");

    public MechanicalUserGUIScreen(MechanicalUserGUIMenu menu, Inventory inventory, Component title) {
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
        graphics.blit(UPGRADE_ICON, leftPos + 8, topPos + 19, 0, 0, 16, 16, 16, 16);
        graphics.blit(ENERGY_HOLDER, leftPos + 150, topPos + 7, 0, 0, 18, 72, 18, 72);
        graphics.blit(ENERGY_FILL, leftPos + 151, topPos + 8, 0, 0, 16, 70, 16, 70);

        BlockEntity user = menu.world.getBlockEntity(menu.position);
        IEnergyStorage storage = user == null ? null : user.getCapability(ForgeCapabilities.ENERGY).orElse(null);
        int filled = storage == null || storage.getMaxEnergyStored() <= 0 ? 0
                : (int) (70L * storage.getEnergyStored() / storage.getMaxEnergyStored());
        filled = Mth.clamp(filled, 0, 70);
        if (filled < 70) {
            graphics.blit(ENERGY_EMPTY, leftPos + 151, topPos + 8, 0, 0, 16, 70 - filled, 16, 70);
        }
        RenderSystem.disableBlend();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 7, 5, 0x404040, false);
        graphics.drawString(font, menu.modeLabel(), 51, 67, 0x404040, false);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Toggle"), button -> {
            if (minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        }).bounds(leftPos + 7, topPos + 61, 40, 20).build());
    }
}
