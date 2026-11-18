
package inzhefop.extrautilitiesrebirth.item;

import inzhefop.extrautilitiesrebirth.block.entity.FuelGeneratorBlockEntity;
import inzhefop.extrautilitiesrebirth.block.entity.MagmaGeneratorBlockEntity;
import inzhefop.extrautilitiesrebirth.block.entity.RainbowGeneratorBlockEntity;
import inzhefop.extrautilitiesrebirth.block.entity.WirelessFEBatteryBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionResult;

import net.minecraft.network.chat.Component;

import java.util.List;

public class WrenchItem extends Item {
	private static final String RECEIVER_X = "reciever_x";
	private static final String RECEIVER_Y = "reciever_y";
	private static final String RECEIVER_Z = "reciever_z";
	private static final String RECEIVER_DIMENSION = "receiverDimension";

	public WrenchItem() {
		super(new Item.Properties().stacksTo(1).rarity(Rarity.COMMON));
	}

	@Override
	public UseAnim getUseAnimation(ItemStack itemstack) {
		return UseAnim.EAT;
	}

	@Override
	public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
		super.appendHoverText(itemstack, world, list, flag);
		list.add(Component.literal("\u00A77Sneak-right-click a Rainbow Generator to select it."));
		list.add(Component.literal("\u00A77Right-click each fuel generator to link it."));
		list.add(Component.translatable("tooltip.extrautilitiesrebirth.wireless.link").withStyle(ChatFormatting.GRAY));
	}

	@Override
	public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
		Player player = context.getPlayer();
		if (player == null) {
			return InteractionResult.PASS;
		}
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockEntity target = level.getBlockEntity(pos);
		if (target instanceof RainbowGeneratorBlockEntity && player.isSecondaryUseActive()) {
			if (!level.isClientSide) {
				var selection = stack.getOrCreateTag();
				selection.putDouble("wrenchmode", 1);
				selection.putDouble("x", pos.getX());
				selection.putDouble("y", pos.getY());
				selection.putDouble("z", pos.getZ());
				selection.putString("rainbowDimension", level.dimension().location().toString());
				stack.setHoverName(Component.literal("Wrench [Rainbow Generator at " + pos.toShortString() + "]"));
				player.displayClientMessage(Component.literal("Selected Rainbow Generator at " + pos.toShortString()), true);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		var selection = stack.getTag();
		if ((target instanceof FuelGeneratorBlockEntity || target instanceof MagmaGeneratorBlockEntity)
				&& selection != null && selection.getDouble("wrenchmode") == 1) {
			if (!level.isClientSide) {
				if (!selection.contains("x", Tag.TAG_ANY_NUMERIC) || !selection.contains("y", Tag.TAG_ANY_NUMERIC)
						|| !selection.contains("z", Tag.TAG_ANY_NUMERIC)
						|| (selection.contains("rainbowDimension")
						&& !selection.getString("rainbowDimension").equals(level.dimension().location().toString()))) {
					player.displayClientMessage(Component.literal("Select a Rainbow Generator in this dimension first."), true);
					return InteractionResult.CONSUME;
				}
				BlockPos core = BlockPos.containing(selection.getDouble("x"), selection.getDouble("y"), selection.getDouble("z"));
				if (level.hasChunkAt(core) && !(level.getBlockEntity(core) instanceof RainbowGeneratorBlockEntity)) {
					player.displayClientMessage(Component.literal("The selected Rainbow Generator is missing."), true);
					return InteractionResult.CONSUME;
				}
				var link = target.getPersistentData();
				link.putDouble("rbg_x", core.getX());
				link.putDouble("rbg_y", core.getY());
				link.putDouble("rbg_z", core.getZ());
				target.setChanged();
				player.displayClientMessage(Component.literal("Linked ").append(target.getBlockState().getBlock().getName()), true);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		if (target instanceof WirelessFEBatteryBlockEntity battery) {
			if (level instanceof ServerLevel server) {
				BlockPos receiver = selectedReceiver(selection, level);
				Component result = receiver == null
						? Component.translatable("message.extrautilitiesrebirth.wireless.select_first")
						: battery.changeLink(server, receiver, player.isSecondaryUseActive());
				player.displayClientMessage(result, true);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		if (!(target instanceof RainbowGeneratorBlockEntity || target instanceof FuelGeneratorBlockEntity
				|| target instanceof MagmaGeneratorBlockEntity) && WirelessFEBatteryBlockEntity.findReceiver(target) != null) {
			if (!level.isClientSide) {
				CompoundTag data = stack.getOrCreateTag();
				data.putDouble(RECEIVER_X, pos.getX());
				data.putDouble(RECEIVER_Y, pos.getY());
				data.putDouble(RECEIVER_Z, pos.getZ());
				data.putString(RECEIVER_DIMENSION, level.dimension().location().toString());
				player.displayClientMessage(Component.translatable("message.extrautilitiesrebirth.wireless.selected",
						target.getBlockState().getBlock().getName(), pos.toShortString()), true);
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
		if (target != null && !level.isClientSide) {
			target.getCapability(ForgeCapabilities.FLUID_HANDLER).ifPresent(tanks -> {
				for (int tank = 0; tank < tanks.getTanks(); tank++) {
					var fluid = tanks.getFluidInTank(tank);
					if (!fluid.isEmpty()) {
						player.displayClientMessage(Component.translatable("message.extrautilitiesrebirth.wrench.fluid",
								fluid.getDisplayName(), fluid.getAmount()), false);
					}
				}
			});
		}
		return InteractionResult.PASS;
	}

	private static BlockPos selectedReceiver(CompoundTag tag, Level level) {
		if (tag == null || !tag.contains(RECEIVER_X, Tag.TAG_ANY_NUMERIC) || !tag.contains(RECEIVER_Y, Tag.TAG_ANY_NUMERIC)
				|| !tag.contains(RECEIVER_Z, Tag.TAG_ANY_NUMERIC)
				|| (tag.contains(RECEIVER_DIMENSION) && !tag.getString(RECEIVER_DIMENSION).equals(level.dimension().location().toString()))) {
			return null;
		}
		return BlockPos.containing(tag.getDouble(RECEIVER_X), tag.getDouble(RECEIVER_Y), tag.getDouble(RECEIVER_Z));
	}
}
