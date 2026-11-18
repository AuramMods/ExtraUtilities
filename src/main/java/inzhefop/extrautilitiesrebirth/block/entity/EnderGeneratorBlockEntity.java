package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class EnderGeneratorBlockEntity extends FuelGeneratorBlockEntity {
    public EnderGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.ENDER_GENERATOR.get(), pos, state,
                GeneratorFuel.ENDER, "ender_generator", "Ender Generator");
    }
}
