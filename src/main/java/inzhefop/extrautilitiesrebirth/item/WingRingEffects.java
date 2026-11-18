package inzhefop.extrautilitiesrebirth.item;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = ExtrautilitiesrebirthMod.MODID)
public final class WingRingEffects {
    private static final AttributeModifier SPEED_BOOST = new AttributeModifier(UUID.fromString("1c352541-33d7-49de-8972-39c7c0dbf7b9"),
            "Chicken Wing Ring", 0.1, AttributeModifier.Operation.MULTIPLY_TOTAL);
    private static final double JUMP_BOOST = 0.06;

    private WingRingEffects() {
    }

    @SubscribeEvent
    public static void updateSpeed(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (event.phase != TickEvent.Phase.END || player.level().isClientSide()) {
            return;
        }
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean carried = !getRing(player, ExtrautilitiesrebirthModItems.CHICKEN_WING_RING.get()).isEmpty();
        if (carried && !speed.hasModifier(SPEED_BOOST)) {
            speed.addTransientModifier(SPEED_BOOST);
        } else if (!carried && speed.hasModifier(SPEED_BOOST)) {
            speed.removeModifier(SPEED_BOOST);
        }
    }

    @SubscribeEvent
    public static void boostJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack ring = getRing(player, ExtrautilitiesrebirthModItems.CHICKEN_WING_RING.get());
        if (!ring.isEmpty()) {
            player.setDeltaMovement(player.getDeltaMovement().add(0, JUMP_BOOST, 0));
            if (!player.level().isClientSide()) {
                wearRing(player, ring);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void absorbFall(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()
                || !event.getSource().is(DamageTypes.FALL) || event.getAmount() <= 0 || player.isInvulnerableTo(event.getSource())) {
            return;
        }
        ItemStack ring = getRing(player, ExtrautilitiesrebirthModItems.ADVANCED_CHICKEN_WING_RING.get());
        if (!ring.isEmpty()) {
            event.setCanceled(true);
            wearRing(player, ring);
        }
    }

    private static ItemStack getRing(Player player, Item item) {
        if (player.getOffhandItem().is(item)) {
            return player.getOffhandItem();
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void wearRing(Player player, ItemStack ring) {
        ring.hurtAndBreak(1, player, holder -> holder.getInventory().setChanged());
    }
}
