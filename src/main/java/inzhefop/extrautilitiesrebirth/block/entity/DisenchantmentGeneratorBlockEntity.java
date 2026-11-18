package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class DisenchantmentGeneratorBlockEntity extends FuelGeneratorBlockEntity {
    public DisenchantmentGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.DISENCHANTMENT_GENERATOR.get(), pos, state,
                GeneratorFuel.DISENCHANTMENT, "disenchantment_generator", "Disenchantment Generator");
    }
}
