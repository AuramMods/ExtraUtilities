package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class StoneDrumBlockEntity extends DrumBlockEntity {
    public StoneDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.STONE_DRUM.get(), pos, state, 16_000, false);
    }
}
