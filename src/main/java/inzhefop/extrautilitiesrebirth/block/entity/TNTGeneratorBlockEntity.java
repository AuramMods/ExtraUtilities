package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TNTGeneratorBlockEntity extends FuelGeneratorBlockEntity {
    public TNTGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.TNT_GENERATOR.get(), pos, state,
                GeneratorFuel.TNT, "tnt_generator", "TNT Generator");
    }
}
