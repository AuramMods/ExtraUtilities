package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneGeneratorBlockEntity extends FuelGeneratorBlockEntity {
    public RedstoneGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.REDSTONE_GENERATOR.get(), pos, state,
                GeneratorFuel.REDSTONE, "redstone_generator", "Redstone Generator");
    }
}
