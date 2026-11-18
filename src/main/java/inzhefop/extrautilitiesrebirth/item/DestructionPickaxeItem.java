
package inzhefop.extrautilitiesrebirth.item;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;


import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;

public class DestructionPickaxeItem extends PickaxeItem {
	public DestructionPickaxeItem() {
		super(new Tier() {
			public int getUses() {
				return 6244;
			}

			public float getSpeed() {
				return 9f;
			}

			public float getAttackDamageBonus() {
				return 3f;
			}

			public int getLevel() {
				return 4;
			}

			public int getEnchantmentValue() {
				return 6;
			}

			public Ingredient getRepairIngredient() {
				return Ingredient.of(new ItemStack(ExtrautilitiesrebirthModItems.UNSTABLE_INGOT.get()));
			}
		}, 1, -3f, new Item.Properties().fireResistant());
	}

	public static boolean clears(BlockState state) {
		return state.is(Blocks.STONE) || state.is(Blocks.COBBLESTONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.COBBLED_DEEPSLATE);
	}

	@Override
	public float getDestroySpeed(ItemStack stack, BlockState state) {
		return clears(state) ? Tiers.DIAMOND.getSpeed() * 5 : 0.2f;
	}

}
