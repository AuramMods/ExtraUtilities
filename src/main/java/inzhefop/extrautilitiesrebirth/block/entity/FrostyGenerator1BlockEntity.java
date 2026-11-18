package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class FrostyGenerator1BlockEntity extends FuelGeneratorBlockEntity {
    public FrostyGenerator1BlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.FROSTY_GENERATOR_1.get(), pos, state,
                GeneratorFuel.FROSTY, "frosty_generator_1", "Frosty Generator");
    }
}
