package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class SurvivalGeneratorBlockEntity extends FuelGeneratorBlockEntity {
    public SurvivalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.SURVIVAL_GENERATOR.get(), pos, state,
                GeneratorFuel.SURVIVAL, "survival_generator", "Survival Generator");
    }
}
