package inzhefop.extrautilitiesrebirth.item;

import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ExtrautilitiesrebirthMod.MODID)
public class DropofEvilItem extends Item {
    public DropofEvilItem() {
        super(new Properties());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var player = context.getPlayer();
        if (!level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
            return InteractionResult.PASS;
        }
        if (player != null && (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            if (!level.setBlockAndUpdate(pos, ExtrautilitiesrebirthModBlocks.CURSED_EARTH.get().defaultBlockState())) {
                return InteractionResult.FAIL;
            }
            if (player == null || !player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void addWitherSkeletonLoot(LivingDropsEvent event) {
        var mob = event.getEntity();
        var level = mob.level();
        if (mob.getType() == EntityType.WITHER_SKELETON && !level.isClientSide()
                && level.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)
                && mob.getRandom().nextFloat() < 0.1f * (1 + event.getLootingLevel())) {
            var item = new ItemStack(ExtrautilitiesrebirthModItems.DROPOF_EVIL.get());
            event.getDrops().add(new ItemEntity(level, mob.getX(), mob.getY(), mob.getZ(), item));
        }
    }
}
