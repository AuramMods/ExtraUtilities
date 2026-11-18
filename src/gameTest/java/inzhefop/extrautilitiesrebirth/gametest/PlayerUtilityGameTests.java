package inzhefop.extrautilitiesrebirth.gametest;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ExtrautilitiesrebirthMod.MODID)
@PrefixGameTestTemplate(false)
public class PlayerUtilityGameTests {
    @GameTest(template = "machine_test")
    public static void chickenWingRingBoostsMovementOnlyWhileCarried(GameTestHelper test) {
        var player = test.makeMockSurvivalPlayer();
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var normal = speed.getValue();
        var first = new ItemStack(ExtrautilitiesrebirthModItems.CHICKEN_WING_RING.get());
        var second = first.copy();
        player.getInventory().setItem(9, first);
        player.getInventory().setItem(10, second);
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        test.assertTrue(Math.abs(speed.getValue() - 1.1 * normal) < 0.0001, "Two carried rings should grant a single ten percent speed bonus");
        player.setDeltaMovement(0, 0.42, 0);
        ForgeHooks.onLivingJump(player);
        test.assertTrue(player.getDeltaMovement().y > 0.42 && first.getDamageValue() == 1 && second.getDamageValue() == 0,
                "A jump should be boosted and charge only one carried ring");
        player.getInventory().setItem(9, ItemStack.EMPTY);
        player.getInventory().setItem(10, ItemStack.EMPTY);
        MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        test.assertTrue(speed.getValue() == normal, "Removing the rings should remove the speed modifier");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void advancedRingPreventsFallDamageAndWearsOnlyWhenItHelps(GameTestHelper test) {
        var level = test.getLevel();
        var player = test.makeMockSurvivalPlayer();
        var ring = new ItemStack(ExtrautilitiesrebirthModItems.ADVANCED_CHICKEN_WING_RING.get());
        player.setItemInHand(InteractionHand.OFF_HAND, ring);
        var health = player.getHealth();
        player.hurt(level.damageSources().fall(), 6);
        test.assertTrue(player.getHealth() == health && ring.getDamageValue() == 1, "The carried ring should absorb the fall and cost one durability");
        player.hurt(level.damageSources().generic(), 2);
        test.assertTrue(player.getHealth() == health - 2 && ring.getDamageValue() == 1, "Non-fall damage should bypass the ring and leave its durability alone");
        test.succeed();
    }

    @GameTest(template = "machine_test")
    public static void namedBossesDropStableSigilsAndCraftingRetainsItemData(GameTestHelper test) {
        var level = test.getLevel();
        var pos = new BlockPos(3, 2, 3);
        var wither = test.spawn(EntityType.WITHER, pos);
        wither.setCustomName(Component.literal("Test Wither"));
        wither.hurt(level.damageSources().genericKill(), Float.MAX_VALUE);
        var item = ExtrautilitiesrebirthModItems.DEVISION_SIGIL.get();
        test.assertTrue(test.getEntities(EntityType.ITEM, pos, 2).stream().anyMatch(drop -> drop.getItem().is(item)), "A named Wither should drop a stable sigil");
        var dragon = test.spawn(EntityType.ENDER_DRAGON, pos);
        dragon.setCustomName(Component.literal("Test Dragon"));
        dragon.kill();
        test.runAfterDelay(5, () -> {
            var sigils = test.getEntities(EntityType.ITEM, pos, 4);
            test.assertTrue(sigils.stream().filter(drop -> drop.getItem().is(item)).mapToInt(drop -> drop.getItem().getCount()).sum() == 2,
                    "The dragon should drop exactly one additional sigil");
            var sigil = new ItemStack(item).setHoverName(Component.literal("Keepsake"));
            sigil.setDamageValue(37);
            var remainder = sigil.getCraftingRemainingItem();
            test.assertTrue(remainder.is(item) && remainder.getHoverName().equals(sigil.getHoverName()) && remainder.getDamageValue() == 38,
                    "Stable crafting should cost one durability and preserve the sigil's name");
            test.succeed();
        });
    }

    @GameTest(template = "machine_test")
    public static void magicalAppleFeedsAPlayerThroughVanillaFoodUse(GameTestHelper test) {
        var level = test.getLevel();
        var player = test.makeMockSurvivalPlayer();
        var apples = new ItemStack(ExtrautilitiesrebirthModItems.MAGICAL_APPLE.get(), 2);
        player.getFoodData().setFoodLevel(5);
        player.setItemInHand(InteractionHand.MAIN_HAND, apples);
        test.assertTrue(apples.use(level, player, InteractionHand.MAIN_HAND).getResult().consumesAction(), "A hungry player should be able to start eating the apple");
        var remaining = apples.finishUsingItem(level, player);
        test.assertTrue(remaining.getCount() == 1 && player.getFoodData().getFoodLevel() == 15, "One apple should replenish ten hunger points");
        test.succeed();
    }
}
