package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class PotionGenerator1BlockEntity extends FuelGeneratorBlockEntity {
    public PotionGenerator1BlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.POTION_GENERATOR_1.get(), pos, state,
                GeneratorFuel.POTION, "potion_generator_1", "Potion Generator");
    }
}
