package inzhefop.extrautilitiesrebirth.world.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModItems;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class RedstoneCrystalLootModifier extends LootModifier {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> REGISTRY =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ExtrautilitiesrebirthMod.MODID);
    private static final Codec<RedstoneCrystalLootModifier> CODEC = RecordCodecBuilder.create(instance ->
            codecStart(instance).apply(instance, RedstoneCrystalLootModifier::new));

    static {
        REGISTRY.register("redstone_crystal", () -> CODEC);
    }

    private RedstoneCrystalLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public Codec<RedstoneCrystalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        var state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        var tool = context.getParamOrNull(LootContextParams.TOOL);
        if (!loot.isEmpty() && state != null && tool != null && tool.isCorrectToolForDrops(state)) {
            loot.add(new ItemStack(ExtrautilitiesrebirthModItems.REDSTONE_CRYSTAL.get()));
        }
        return loot;
    }
}
