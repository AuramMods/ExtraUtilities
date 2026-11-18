package inzhefop.extrautilitiesrebirth.item;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExtrautilitiesrebirthMod.MODID)
public final class ArmorPiercingTools {
    private ArmorPiercingTools() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addPiercingDamage(LivingDamageEvent event) {
        var target = event.getEntity();
        var source = event.getSource();
        if (target.level().isClientSide() || event.getAmount() <= 0 || !(source.getDirectEntity() instanceof LivingEntity attacker)
                || !(source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MOB_ATTACK))) {
            return;
        }
        var weapon = attacker.getMainHandItem();
        if (weapon.is(ExtrautilitiesrebirthModItems.KIKOKU.get())) {
            event.setAmount(event.getAmount() + 4);
        } else if (weapon.is(ExtrautilitiesrebirthModItems.ETHERIC_SWORD.get())) {
            event.setAmount(event.getAmount() + target.getArmorValue() * 0.25f);
        }
    }
}
