package inzhefop.extrautilitiesrebirth.item;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.context.UseOnContext;

public class ReinforcedGlassCutterItem extends Item {
    public ReinforcedGlassCutterItem() {
        super(new Properties().rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return GlassCutting.use(context, 0);
    }
}
