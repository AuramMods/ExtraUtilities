package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class CreativeDrumBlockEntity extends DrumBlockEntity {
    public CreativeDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.CREATIVE_DRUM.get(), pos, state, Integer.MAX_VALUE, true);
    }
}
