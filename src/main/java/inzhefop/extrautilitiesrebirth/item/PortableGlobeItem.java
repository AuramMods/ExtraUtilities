package inzhefop.extrautilitiesrebirth.item;

import inzhefop.extrautilitiesrebirth.procedures.OverworldPortalOnBlockRightClickedProcedure;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;

public class PortableGlobeItem extends Item {
    private static final int RETURN_COOLDOWN = 12_000;

    public PortableGlobeItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)
                || !OverworldPortalOnBlockRightClickedProcedure.use(player, player.blockPosition())) {
            return InteractionResultHolder.pass(stack);
        }
        if (!level.isClientSide() && !level.dimension().equals(Level.OVERWORLD)) {
            player.getCooldowns().addCooldown(this, RETURN_COOLDOWN);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
