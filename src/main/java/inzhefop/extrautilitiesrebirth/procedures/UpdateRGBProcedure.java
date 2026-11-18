package inzhefop.extrautilitiesrebirth.procedures;

import inzhefop.extrautilitiesrebirth.block.entity.RainbowGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

public class UpdateRGBProcedure {
    public static void execute(ServerLevel level, BlockEntity generator, String key) {
        var link = generator.getPersistentData();
        if (!link.contains("rbg_x", Tag.TAG_ANY_NUMERIC) || !link.contains("rbg_y", Tag.TAG_ANY_NUMERIC)
                || !link.contains("rbg_z", Tag.TAG_ANY_NUMERIC)) {
            return;
        }
        BlockPos core = BlockPos.containing(link.getDouble("rbg_x"), link.getDouble("rbg_y"), link.getDouble("rbg_z"));
        if (level.hasChunkAt(core) && level.getBlockEntity(core) instanceof RainbowGeneratorBlockEntity rainbow) {
            rainbow.getPersistentData().putDouble(key, 1);
        }
    }
}
