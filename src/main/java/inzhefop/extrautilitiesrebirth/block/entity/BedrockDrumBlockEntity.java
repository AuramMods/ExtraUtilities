package inzhefop.extrautilitiesrebirth.block.entity;

import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class BedrockDrumBlockEntity extends DrumBlockEntity {
    public BedrockDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ExtrautilitiesrebirthModBlockEntities.BEDROCK_DRUM.get(), pos, state, 65_536_000, false);
    }
}
