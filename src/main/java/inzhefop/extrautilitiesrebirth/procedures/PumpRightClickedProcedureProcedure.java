package inzhefop.extrautilitiesrebirth.procedures;

import inzhefop.extrautilitiesrebirth.block.entity.EnderThermicPumpBlockEntity;
import inzhefop.extrautilitiesrebirth.init.ExtrautilitiesrebirthModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PumpRightClickedProcedureProcedure {
    private static final Direction[] MARKER_SIDES = {
            Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH
    };

    public static void execute(Level level, BlockPos pos, Player player) {
        if (level.isClientSide || !(level.getBlockEntity(pos) instanceof EnderThermicPumpBlockEntity pump)) {
            return;
        }

        CompoundTag data = pump.getPersistentData();
        switch (data.getInt("state")) {
            case PumpUpdateTickProcedure.ACTIVE ->
                    player.displayClientMessage(Component.literal("Pump active. Scanned " + data.getInt("minedblocks") + " blocks."), true);
            case PumpUpdateTickProcedure.FINISHED ->
                    player.displayClientMessage(Component.literal("The pump has finished scanning."), true);
            case PumpUpdateTickProcedure.BLOCKED ->
                    player.displayClientMessage(Component.literal("The tank cannot fit the next bucket."), true);
            default -> {
                for (Direction side : MARKER_SIDES) {
                    BlockPos adjacent = pos.relative(side);
                    if (!level.getBlockState(adjacent).is(ExtrautilitiesrebirthModBlocks.ENDER_MARKER.get())) {
                        continue;
                    }
                    var marker = level.getBlockEntity(adjacent);
                    if (marker != null && marker.getPersistentData().getBoolean("complete")) {
                        activate(level, pump, marker.getPersistentData());
                        player.displayClientMessage(Component.literal("Ender Marker boundary linked. Pump started."), true);
                        return;
                    }
                }
                player.displayClientMessage(Component.literal("Place the pump next to a linked Ender Marker corner."), true);
            }
        }
    }

    private static void activate(Level level, EnderThermicPumpBlockEntity pump, CompoundTag marker) {
        BlockPos pos = pump.getBlockPos();
        CompoundTag data = pump.getPersistentData();
        int top = pos.getY() - 1;
        data.putBoolean("attachedmarker", true);
        data.putDouble("state", PumpUpdateTickProcedure.ACTIVE);
        data.putDouble("areaminx", marker.getDouble("minx"));
        data.putDouble("areamaxx", marker.getDouble("maxx"));
        data.putDouble("areaminz", marker.getDouble("minz"));
        data.putDouble("areamaxz", marker.getDouble("maxz"));
        data.putDouble("areamaxy", top);
        data.putDouble("xcoord", marker.getDouble("minx"));
        data.putDouble("ycoord", top);
        data.putDouble("zcoord", marker.getDouble("minz"));
        data.putDouble("areadirect1", marker.getDouble("direc1"));
        data.putDouble("areadirect2", marker.getDouble("direc2"));
        data.putDouble("ticks", 0);
        EnderQuarryNeighbourBlockChangesProcedure.execute(level, pos.getX(), pos.getY(), pos.getZ());
        pump.setChanged();
    }
}
