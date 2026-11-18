package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class CulinaryGeneratorBlockEntity extends FuelGeneratorBlockEntity {
    public CulinaryGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.CULINARY_GENERATOR.get(), pos, state,
                GeneratorFuel.CULINARY, "culinary_generator", "Culinary Generator");
    }
}
