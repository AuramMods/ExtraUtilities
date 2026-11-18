package inzhefop.extrautilitiesrebirth.procedures;

import inzhefop.extrautilitiesrebirth.network.ExtrautilitiesrebirthModVariables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public class OverworldPortalOnBlockRightClickedProcedure {
    public static boolean use(Player player, BlockPos returnPos) {
        if (player.level().isClientSide()) {
            return true;
        }
        var capability = player.getCapability(ExtrautilitiesrebirthModVariables.PLAYER_VARIABLES_CAPABILITY);
        if (player.level().dimension().equals(Level.OVERWORLD)) {
            return capability.map(data -> {
                data.setOverworldReturn(returnPos);
                data.syncPlayerVariables(player);
                player.displayClientMessage(Component.translatable("message.extrautilitiesrebirth.overworld_return_set"), false);
                return true;
            }).orElse(false);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        var overworld = serverPlayer.server.overworld();
        var target = capability.map(ExtrautilitiesrebirthModVariables.PlayerVariables::getOverworldReturn).orElse(null);
        var landing = findLanding(overworld, player, target);
        return serverPlayer.teleportTo(overworld, landing.x, landing.y, landing.z, Set.of(), player.getYRot(), player.getXRot());
    }

    private static Vec3 findLanding(ServerLevel level, Player player, BlockPos saved) {
        if (saved != null && level.getWorldBorder().isWithinBounds(saved)) {
            if (canLand(level, player, saved)) {
                return Vec3.atBottomCenterOf(saved);
            }
            var surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, saved);
            if (canLand(level, player, surface)) {
                return Vec3.atBottomCenterOf(surface);
            }
        }
        var spawn = level.getSharedSpawnPos();
        var spawnSurface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn);
        return Vec3.atBottomCenterOf(canLand(level, player, spawn) ? spawn : spawnSurface);
    }

    private static boolean canLand(ServerLevel level, Player player, BlockPos pos) {
        return level.isInWorldBounds(pos) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)
                && level.getFluidState(pos).isEmpty() && level.getFluidState(pos.below()).isEmpty()
                && level.noCollision(player, player.getDimensions(player.getPose()).makeBoundingBox(Vec3.atBottomCenterOf(pos)));
    }
}
