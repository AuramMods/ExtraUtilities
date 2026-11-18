
package inzhefop.extrautilitiesrebirth.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.HoeItem;


import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;

public class ReversingHoeItem extends HoeItem {
	public ReversingHoeItem() {
		super(new Tier() {
			public int getUses() {
				return 1561;
			}

			public float getSpeed() {
				return 12f;
			}

			public float getAttackDamageBonus() {
				return 7f;
			}

			public int getLevel() {
				return 4;
			}

			public int getEnchantmentValue() {
				return 22;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of(new ItemStack(ExtrautilitiesrebirthModItems.UNSTABLE_INGOT.get()));
			}
		}, 0, 16f, new Item.Properties());
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		var player = context.getPlayer();
		var level = context.getLevel();
		var pos = context.getClickedPos();
		var state = level.getBlockState(pos);
		if (player == null || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand())) {
			return InteractionResult.FAIL;
		}
		var restored = reverse(state);
		if (restored == null) {
			return super.useOn(context);
		}
		if (!level.isClientSide()) {
			if (!level.setBlock(pos, restored, Block.UPDATE_ALL)) {
				return InteractionResult.FAIL;
			}
			context.getItemInHand().hurtAndBreak(1, player, holder -> holder.broadcastBreakEvent(context.getHand()));
			level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, restored));
		}
		return InteractionResult.sidedSuccess(level.isClientSide());
	}

	private static BlockState reverse(BlockState state) {
		if (state.getBlock() instanceof CropBlock crop && crop.getAge(state) > 0) {
			return crop.getStateForAge(crop.getAge(state) - 1);
		}
		if (state.is(Blocks.DIRT)) {
			return Blocks.GRASS_BLOCK.defaultBlockState();
		}
		return state.is(Blocks.COBBLESTONE) ? Blocks.STONE.defaultBlockState() : null;
	}
}
