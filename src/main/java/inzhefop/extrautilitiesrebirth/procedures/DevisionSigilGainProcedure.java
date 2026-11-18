package inzhefop.extrautilitiesrebirth.procedures;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExtrautilitiesrebirthMod.MODID)
public class DevisionSigilGainProcedure {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void witherDropsSigil(LivingDropsEvent event) {
        var entity = event.getEntity();
        if (entity.getType() == EntityType.WITHER && entity.level() instanceof ServerLevel level
                && level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            event.getDrops().add(createDrop(level, entity));
        }
    }

    @SubscribeEvent
    public static void dragonDropsSigil(EntityLeaveLevelEvent event) {
        var entity = event.getEntity();
        if (entity.getType() == EntityType.ENDER_DRAGON && entity.getRemovalReason() == Entity.RemovalReason.KILLED
                && event.getLevel() instanceof ServerLevel level && level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            var drop = createDrop(level, entity);
            var server = level.getServer();
            // Drop the sigil after the dragon's removal has finished updating the entity lists.
            server.tell(new TickTask(server.getTickCount(), () -> level.addFreshEntity(drop)));
        }
    }

    private static ItemEntity createDrop(ServerLevel level, Entity entity) {
        var stack = new ItemStack(ExtrautilitiesrebirthModItems.DEVISION_SIGIL.get());
        var drop = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), stack);
        drop.setDefaultPickUpDelay();
        drop.setUnlimitedLifetime();
        return drop;
    }
}
