package inzhefop.extrautilitiesrebirth.procedures;

import inzhefop.extrautilitiesrebirth.block.entity.MagmaGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public class MagmaGeneratorOnBlockRightClickedProcedure {
    public static InteractionResult execute(Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!(level.getBlockEntity(pos) instanceof MagmaGeneratorBlockEntity generator)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.LAVA_BUCKET)) {
            if (!level.isClientSide && generator.fillLavaBucket() && !player.getAbilities().instabuild) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.BUCKET)));
            }
        } else {
            if (hand == InteractionHand.MAIN_HAND && player.getOffhandItem().is(Items.LAVA_BUCKET)) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, generator, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
