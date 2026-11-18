
package inzhefop.extrautilitiesrebirth.item;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.AxeItem;


import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;

@Mod.EventBusSubscriber(modid = ExtrautilitiesrebirthMod.MODID)
public class HealingAxeItem extends AxeItem {
	public HealingAxeItem() {
		super(new Tier() {
			public int getUses() {
				return 1561;
			}

			public float getSpeed() {
				return 12f;
			}

			public float getAttackDamageBonus() {
				return 4f;
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

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public static void transferHealth(AttackEntityEvent event) {
		var player = event.getEntity();
		var axe = player.getMainHandItem();
		if (!(axe.getItem() instanceof HealingAxeItem) || !(event.getTarget() instanceof LivingEntity target) || !target.isAlive()) {
			return;
		}
		event.setCanceled(true);
		if (player.level().isClientSide()) {
			return;
		}
		float available = Math.min(2, player.getHealth() - 1);
		if (available <= 0) {
			return;
		}
		float transferred;
		if (target.isInvertedHealAndHarm()) {
			transferred = target.hurt(player.damageSources().indirectMagic(player, player), available * 4) ? available : 0;
		} else {
			float initialHealth = target.getHealth();
			target.heal(available * 1.5f);
			transferred = Math.min(available, (target.getHealth() - initialHealth) / 1.5f);
		}
		if (transferred > 0) {
			player.setHealth(player.getHealth() - transferred);
			axe.hurtAndBreak(1, player, holder -> holder.broadcastBreakEvent(InteractionHand.MAIN_HAND));
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
		if (!level.isClientSide() && entity instanceof Player player && player.tickCount % 40 == 0 && player.getFoodData().needsFood()
				&& (selected || (player.getOffhandItem() == stack && !(player.getMainHandItem().getItem() instanceof HealingAxeItem)))) {
			player.getFoodData().eat(1, 0);
		}
	}

}
