package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class SlimeyGeneratorBlockEntity extends FuelGeneratorBlockEntity {
    public SlimeyGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.SLIMEY_GENERATOR.get(), pos, state,
                GeneratorFuel.SLIMEY, "slimey_generator", "Slimey Generator");
    }
}
