package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class DeathGeneratorBlockEntity extends FuelGeneratorBlockEntity {
    public DeathGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.DEATH_GENERATOR.get(), pos, state,
                GeneratorFuel.DEATH, "death_generator", "Death Generator");
    }
}
