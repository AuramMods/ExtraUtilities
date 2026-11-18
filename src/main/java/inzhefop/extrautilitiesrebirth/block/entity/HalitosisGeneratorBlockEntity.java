package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class HalitosisGeneratorBlockEntity extends FuelGeneratorBlockEntity {
    public HalitosisGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.HALITOSIS_GENERATOR.get(), pos, state,
                GeneratorFuel.HALITOSIS, "halitosis_generator", "Halitosis Generator");
    }
}
