package inzhefop.extrautilitiesrebirth.procedures;

import inzhefop.extrautilitiesrebirth.block.entity.RainbowGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class RainbowGeneratorUpdateTickProcedure {
    private static final int POWER = 25_000_000;
    private static final List<String> GENERATORS = List.of(
            "culinary", "death", "disenchantment", "ender", "tnt", "frosty", "furnace", "halitosis",
            "neatherstar", "overclocked", "pink", "potion", "slimey", "survival", "redstone", "magma");

    public static void execute(ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof RainbowGeneratorBlockEntity generator)) {
            return;
        }

        CompoundTag data = generator.getPersistentData();
        int count = 0;
        boolean changed = false;
        for (String key : GENERATORS) {
            boolean active = data.getDouble(key) >= 1;
            if (active) {
                count++;
                data.putDouble(key, 0);
            }
            String status = key + "_machine";
            if (active != data.getBoolean(status)) {
                data.putBoolean(status, active);
                changed = true;
            }
        }

        boolean generating = count == GENERATORS.size();
        String runningCount = Integer.toString(count);
        if (changed || generating != data.getBoolean("rgb_running") || !runningCount.equals(data.getString("running_gen"))) {
            data.putBoolean("rgb_running", generating);
            data.putString("running_gen", runningCount);
            generator.setChanged();
            var state = generator.getBlockState();
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
        }

        if (generating) {
            GeneratorOutput.storeGeneratedEnergy(level, pos, POWER);
        }
        GeneratorOutput.transferStoredEnergy(level, generator, POWER);
    }
}
