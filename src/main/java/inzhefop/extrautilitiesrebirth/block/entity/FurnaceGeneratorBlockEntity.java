package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class FurnaceGeneratorBlockEntity extends FuelGeneratorBlockEntity {
    public FurnaceGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.FURNACE_GENERATOR.get(), pos, state,
                GeneratorFuel.FURNACE, "furnace_generator", "Furnace Generator");
    }
}
