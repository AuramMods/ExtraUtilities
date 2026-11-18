package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class IronDrumBlockEntity extends DrumBlockEntity {
    public IronDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.IRON_DRUM.get(), pos, state, 256_000, false);
    }
}
