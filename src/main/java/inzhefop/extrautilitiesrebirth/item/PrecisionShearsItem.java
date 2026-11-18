package inzhefop.extrautilitiesrebirth.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;

public class PrecisionShearsItem extends ShearsItem {
    public PrecisionShearsItem() {
        super(new Properties().durability(0));
    }

    @Override
    public int getEnchantmentValue() {
        return 2;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return 8f;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        var level = context.getLevel();
        var pos = context.getClickedPos();
        var state = level.getBlockState(pos);
        if (player == null || !player.isShiftKeyDown() || state.isAir() || state.hasBlockEntity()
                || state.getDestroySpeed(level, pos) < 0 || !TierSortingRegistry.isCorrectTierForDrops(Tiers.STONE, state)) {
            return InteractionResult.PASS;
        }
        var picked = new ItemStack(state.getBlock());
        if (picked.isEmpty()) {
            return InteractionResult.PASS;
        }
        var inventory = player.getInventory();
        if (inventory.getFreeSlot() < 0 && inventory.getSlotWithRemainingSpace(picked) < 0) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel serverLevel) {
            if (!ToolHarvest.breakBlock(serverLevel, pos, state, player, context.getClickedFace(), stack)) {
                return InteractionResult.FAIL;
            }
            if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
                inventory.placeItemBackInInventory(picked);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
