package inzhefop.extrautilitiesrebirth.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.function.Consumer;

public final class ChunkBlocks {
    private ChunkBlocks() {
    }

    public static void forEach(LevelChunk chunk, Block block, Consumer<BlockPos> action) {
        var chunkPos = chunk.getPos();
        var sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            var section = sections[index];
            if (!section.maybeHas(state -> state.is(block))) {
                continue;
            }
            int y = chunk.getSectionYFromSectionIndex(index) * 16;
            for (BlockPos cursor : BlockPos.betweenClosed(0, 0, 0, 15, 15, 15)) {
                if (section.getBlockState(cursor.getX(), cursor.getY(), cursor.getZ()).is(block)) {
                    action.accept(new BlockPos(chunkPos.getMinBlockX() + cursor.getX(), y + cursor.getY(), chunkPos.getMinBlockZ() + cursor.getZ()));
                }
            }
        }
    }
}
