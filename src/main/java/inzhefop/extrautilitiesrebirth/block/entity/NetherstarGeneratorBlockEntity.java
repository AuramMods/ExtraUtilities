package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class NetherstarGeneratorBlockEntity extends FuelGeneratorBlockEntity {
    public NetherstarGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.NETHERSTAR_GENERATOR.get(), pos, state,
                GeneratorFuel.NETHERSTAR, "netherstar_generator", "Netherstar Generator");
    }
}
