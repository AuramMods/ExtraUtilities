
package inzhefop.extrautilitiesrebirth.item;

import inzhefop.extrautilitiesrebirth.block.entity.FuelGeneratorBlockEntity;
import inzhefop.extrautilitiesrebirth.block.entity.MagmaGeneratorBlockEntity;
import inzhefop.extrautilitiesrebirth.block.entity.RainbowGeneratorBlockEntity;
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

import inzhefop.extrautilitiesrebirth.procedures.WrenchClickedOnBlockProcedureProcedure;


public class WrenchItem extends Item {
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
		list.add(Component.literal("\u00A77*Connects Wireless Battery and reciever!"));
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
		return InteractionResult.PASS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		InteractionResult retval = super.useOn(context);
		WrenchClickedOnBlockProcedureProcedure.execute(context.getLevel(), context.getClickedPos().getX(), context.getClickedPos().getY(),
				context.getClickedPos().getZ(), context.getPlayer(), context.getItemInHand());
		return retval;
	}
}
