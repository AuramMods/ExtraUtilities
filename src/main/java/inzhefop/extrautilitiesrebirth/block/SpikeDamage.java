package inzhefop.extrautilitiesrebirth.block;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExtrautilitiesrebirthMod.MODID)
enum SpikeDamage {
    WOOD(0.5f), STONE(1.2f), IRON(2), GOLD(5), DIAMOND(12), CREATIVE(1_000);

    private static final ResourceKey<DamageType> GOLD_SPIKE = key("gold_spike");
    private static final ResourceKey<DamageType> DIAMOND_SPIKE = key("diamond_spike");
    private final float damage;

    SpikeDamage(float damage) {
        this.damage = damage;
    }

    void apply(Level level, Entity entity) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        float amount = this == WOOD ? Math.min(damage, living.getHealth() - 1) : damage;
        if (amount > 0) {
            living.hurt(damageSource(serverLevel, living), amount);
        }
    }

    private DamageSource damageSource(ServerLevel level, LivingEntity target) {
        var types = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
        if (this == GOLD) {
            return new DamageSource(types.getHolderOrThrow(GOLD_SPIKE));
        }
        if (this == DIAMOND && target instanceof Mob) {
            return new DamageSource(types.getHolderOrThrow(DIAMOND_SPIKE), FakePlayerFactory.getMinecraft(level));
        }
        return target.damageSources().generic();
    }

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.tryBuild(ExtrautilitiesrebirthMod.MODID, name));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void handleExperience(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        if (event.getSource().is(DIAMOND_SPIKE)) {
            mob.skipDropExperience();
        } else if (event.getSource().is(GOLD_SPIKE) && mob.shouldDropExperience()
                && !mob.wasExperienceConsumed() && level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            mob.skipDropExperience();
            Player credit = mob.getKillCredit() instanceof Player player ? player : null;
            ExperienceOrb.award(level, mob.position(), ForgeEventFactory.getExperienceDrop(mob, credit, mob.getExperienceReward()));
        }
    }
}
