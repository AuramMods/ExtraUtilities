package inzhefop.extrautilitiesrebirth.world.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import inzhefop.extrautilitiesrebirth.item.DestructionPickaxeItem;
import inzhefop.extrautilitiesrebirth.item.ErosionShovelItem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.LootModifier;

public final class VoidHarvestLootModifier extends LootModifier {
    public static final Codec<VoidHarvestLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance).apply(instance, VoidHarvestLootModifier::new));

    private VoidHarvestLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public Codec<VoidHarvestLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        var state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        var tool = context.getParamOrNull(LootContextParams.TOOL);
        if (state != null && tool != null && ((tool.getItem() instanceof DestructionPickaxeItem && DestructionPickaxeItem.clears(state))
                || (tool.getItem() instanceof ErosionShovelItem && ErosionShovelItem.clears(state)))) {
            loot.clear();
        }
        return loot;
    }
}
