package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ReinforcedDrumBlockEntity extends DrumBlockEntity {
    public ReinforcedDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.REINFORCED_DRUM.get(), pos, state, 4_096_000, false);
    }
}
