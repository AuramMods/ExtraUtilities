package inzhefop.extrautilitiesrebirth.client.gui;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;

import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics; // Import GuiGraphics
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.util.Mth;
import net.minecraftforge.energy.IEnergyStorage;

import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.common.capabilities.ForgeCapabilities; // Import ForgeCapabilities

import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

import inzhefop.extrautilitiesrebirth.world.inventory.NormalGUIMenu;

import com.mojang.blaze3d.systems.RenderSystem;

public class NormalGUIScreen extends AbstractContainerScreen<NormalGUIMenu> {
	private final Level world;
	private final BlockPos position;
	private Button redstoneButton;
	private boolean redstoneRequired;

	public NormalGUIScreen(NormalGUIMenu container, Inventory inventory, Component text) {
		super(container, inventory, text);
		this.world = container.world;
		this.position = container.position;
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	private static final ResourceLocation texture = new ResourceLocation("extrautilitiesrebirth:textures/screens/normal_gui.png");

	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(guiGraphics);
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(guiGraphics, mouseX, mouseY);
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int gx, int gy) {
		RenderSystem.setShaderColor(1, 1, 1, 1);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		// Main Background
		guiGraphics.blit(texture, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, this.imageWidth, this.imageHeight);

		// Upgrade Icon
		guiGraphics.blit(new ResourceLocation("extrautilitiesrebirth:textures/upgrade_icon.png"),
				this.leftPos + 7, this.topPos + 18, 0, 0, 16, 16, 16, 16);

		boolean cross = new Object() {
			public boolean getValue(LevelAccessor world, BlockPos pos, String tag) {
				BlockEntity blockEntity = world.getBlockEntity(pos);
				if (blockEntity != null)
					return blockEntity.getPersistentData().getBoolean(tag);
				return false;
			}
		}.getValue(world, position, "valid");

		// Progress Arrow Background
		guiGraphics.blit(new ResourceLocation("extrautilitiesrebirth:textures/progress_arrow_back.png"),
				this.leftPos + 83, this.topPos + 36, 0, 0, 22, 15, 22, 15);

		double progress = 0;
		progress = new Object() {
			public double getValue(LevelAccessor world, BlockPos pos, String tag) {
				BlockEntity blockEntity = world.getBlockEntity(pos);
				if (blockEntity != null)
					return blockEntity.getPersistentData().getDouble(tag);
				return -1;
			}
		}.getValue(world, position, "progress2");

		int prg = 22 - Mth.clamp((int) progress, 0, 22);

		// Progress Arrow Fill
		if (prg > 0) {
			guiGraphics.blit(new ResourceLocation("extrautilitiesrebirth:textures/progress_arrow_fill.png"),
					this.leftPos + 83, this.topPos + 36, 0, 0, prg, 15, 22, 15);
		}

		// Progress Arrow Negative
		guiGraphics.blit(new ResourceLocation("extrautilitiesrebirth:textures/progress_arrow_negative.png"),
				this.leftPos + 83, this.topPos + 36, 0, 0, 22, 15, 22, 15);

		if (!cross) {
			guiGraphics.blit(new ResourceLocation("extrautilitiesrebirth:textures/kreuz.png"),
					this.leftPos + 87, this.topPos + 36, 0, 0, 16, 16, 16, 16);
		}

		// Energy Bar Holder
		guiGraphics.blit(new ResourceLocation("extrautilitiesrebirth:textures/energy_bar_holder.png"),
				this.leftPos + 150, this.topPos + 7, 0, 0, 18, 72, 18, 72);

		BlockEntity machine = world.getBlockEntity(position);
		IEnergyStorage storage = machine == null ? null : machine.getCapability(ForgeCapabilities.ENERGY).orElse(null);
		int pwr = storage == null || storage.getMaxEnergyStored() <= 0 ? 0
				: (int) (70L * storage.getEnergyStored() / storage.getMaxEnergyStored());
		pwr = Mth.clamp(pwr, 0, 70);

		// Energy Bar Fill
		guiGraphics.blit(new ResourceLocation("extrautilitiesrebirth:textures/energybarfill.png"),
				this.leftPos + 151, this.topPos + 8, 0, 0, 16, 70, 16, 70);

		// Gray Overlay (Inverse Bar)
		if (pwr < 70) {
			guiGraphics.blit(new ResourceLocation("extrautilitiesrebirth:textures/gray.png"),
					this.leftPos + 151, this.topPos + 8, 0, 0, 16, 70 - pwr, 16, 70);
		}

		if ((new Object() {
			public int getFluidTankLevel(LevelAccessor level, BlockPos pos, int tank) {
				AtomicInteger _retval = new AtomicInteger(0);
				BlockEntity _ent = level.getBlockEntity(pos);
				if (_ent != null)
					_ent.getCapability(ForgeCapabilities.FLUID_HANDLER, null)
							.ifPresent(capability -> _retval.set(capability.getFluidInTank(tank).getAmount()));
				return _retval.get();
			}
		}.getFluidTankLevel(world, position, 0)) > 0) {
			guiGraphics.blit(new ResourceLocation("extrautilitiesrebirth:textures/lava12x2.png"),
					this.leftPos + 61, this.topPos + 36, 0, 0, 16, 16, 16, 16);
		}

		RenderSystem.disableBlend();
	}

	@Override
	public boolean keyPressed(int key, int b, int c) {
		if (key == 256) {
			this.minecraft.player.closeContainer();
			return true;
		}
		return super.keyPressed(key, b, c);
	}

	@Override
	public void containerTick() {
		super.containerTick();
		if (menu.requiresRedstone() != redstoneRequired) {
			updateRedstoneButton();
		}
	}

	@Override
	protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
		super.renderTooltip(graphics, mouseX, mouseY);
		if (!isHovering(150, 7, 18, 72, mouseX, mouseY)) {
			return;
		}
		BlockEntity machine = world.getBlockEntity(position);
		if (machine == null) {
			return;
		}
		List<Component> lines = new ArrayList<>();
		machine.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> lines.add(
				Component.translatable("gui.extrautilitiesrebirth.generator.stored",
						energy.getEnergyStored(), energy.getMaxEnergyStored())));
		String production = machine.getPersistentData().getString("production");
		String seconds = machine.getPersistentData().getString("seconds");
		if (!production.isEmpty()) {
			lines.add(Component.literal(production));
		}
		if (!seconds.isEmpty()) {
			lines.add(Component.translatable("gui.extrautilitiesrebirth.generator.remaining", seconds));
		}
		graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(this.font, title, 6, 5, -12829636, false);
		guiGraphics.drawString(this.font, Component.literal("Inventory"), 6, 71, -12829636, false);
	}

	@Override
	public void onClose() {
		super.onClose();
	}

	@Override
	protected void init() {
		super.init();
		redstoneButton = addRenderableWidget(Button.builder(Component.empty(), button ->
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0))
				.bounds(leftPos + 108, topPos + 34, 40, 20).build());
		updateRedstoneButton();
	}

	private void updateRedstoneButton() {
		redstoneRequired = menu.requiresRedstone();
		String key = "gui.extrautilitiesrebirth.generator.redstone." + (redstoneRequired ? "required" : "ignored");
		redstoneButton.setMessage(Component.translatable(key));
		redstoneButton.setTooltip(Tooltip.create(Component.translatable(key + ".tooltip")));
	}
}