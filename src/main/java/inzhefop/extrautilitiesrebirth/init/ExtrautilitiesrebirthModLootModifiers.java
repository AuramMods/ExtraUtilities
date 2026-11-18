package inzhefop.extrautilitiesrebirth.init;

import com.mojang.serialization.Codec;
import inzhefop.extrautilitiesrebirth.ExtrautilitiesrebirthMod;
import inzhefop.extrautilitiesrebirth.world.loot.RedstoneCrystalLootModifier;
import inzhefop.extrautilitiesrebirth.world.loot.VoidHarvestLootModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ExtrautilitiesrebirthModLootModifiers {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> REGISTRY =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, ExtrautilitiesrebirthMod.MODID);

    static {
        REGISTRY.register("redstone_crystal", () -> RedstoneCrystalLootModifier.CODEC);
        REGISTRY.register("void_harvest", () -> VoidHarvestLootModifier.CODEC);
    }

    private ExtrautilitiesrebirthModLootModifiers() {
    }
}
