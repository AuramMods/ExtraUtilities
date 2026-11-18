package inzhefop.extrautilitiesrebirth.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

public class GlassCutterItem extends Item {
    public GlassCutterItem() {
        super(new Properties().durability(256));
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        var remainder = stack.copyWithCount(1);
        remainder.setDamageValue(stack.getDamageValue() + 1);
        return remainder.getDamageValue() >= remainder.getMaxDamage() ? ItemStack.EMPTY : remainder;
    }

    @Override
    public boolean isRepairable(ItemStack stack) {
        return false;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return GlassCutting.use(context, 10);
    }
}
