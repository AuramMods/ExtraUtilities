package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class OverclockedGeneratorBlockEntity extends FuelGeneratorBlockEntity {
    public OverclockedGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.OVERCLOCKED_GENERATOR.get(), pos, state,
                GeneratorFuel.OVERCLOCKED, "overclocked_generator", "Overclocked Generator");
    }
}
