
package inzhefop.extrautilitiesrebirth.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;


import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;

public class ErosionShovelItem extends ShovelItem {
	public ErosionShovelItem() {
		super(new Tier() {
			public int getUses() {
				return 6244;
			}

			public float getSpeed() {
				return 9f;
			}

			public float getAttackDamageBonus() {
				return 2f;
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
		}, 1, 1f, new Item.Properties().fireResistant());
	}

	public static boolean clears(BlockState state) {
		return !state.hasBlockEntity() && (state.is(BlockTags.DIRT) || isSandOrGravel(state));
	}

	private static boolean isSandOrGravel(BlockState state) {
		return !state.hasBlockEntity() && (state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL));
	}

	@Override
	public boolean mineBlock(ItemStack stack, Level level, BlockState original, BlockPos pos, LivingEntity entity) {
		boolean result = super.mineBlock(stack, level, original, pos, entity);
		if (level instanceof ServerLevel serverLevel && entity instanceof Player player && clears(original)
				&& level.getBlockState(pos) != original) {
			for (BlockPos above = pos.above(); !stack.isEmpty() && above.getY() < level.getMaxBuildHeight(); above = above.above()) {
				var state = level.getBlockState(above);
				if (!isSandOrGravel(state) || !ToolHarvest.breakBlock(serverLevel, above, state, player, Direction.UP, stack)) {
					break;
				}
				stack.hurtAndBreak(1, player, holder -> holder.broadcastBreakEvent(InteractionHand.MAIN_HAND));
			}
		}
		return result;
	}

}
