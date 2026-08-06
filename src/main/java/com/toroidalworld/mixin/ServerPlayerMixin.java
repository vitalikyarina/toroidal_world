package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.toroidalworld.storage.SeamRespawnData;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

// Reaching a bed is a distance test, and across the seam the plain distance is a whole world: the far half of a bed laid
// over the boundary would always be out of reach.
@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Unique
    private static final double BED_REACH_HORIZONTAL = 3.0;

    @Unique
    private static final double BED_REACH_VERTICAL = 2.0;

    // Where a player's respawn point is written down — by /spawnpoint, by a bed, by a respawn anchor, and by the copy
    // made when death replaces the ServerPlayer. A bed and an anchor are blocks the player stood next to, so they are
    // already inside the world and the wrap costs them the identity; /spawnpoint is the one that can name a point past
    // the bounds, and it must not be the one that decides, because its no-argument form reads the sender's position
    // straight off the command source and never touches a coordinate argument at all.
    //
    // Ahead of NeoForge's spawn-set event rather than behind it: a listener asked where the spawn is going should be
    // shown the point that will actually be stored.
    @ModifyVariable(method = "setRespawnPosition", at = @At("HEAD"), argsOnly = true)
    private ServerPlayer.@Nullable RespawnConfig toroidal$storeRespawnInsideBounds(
            ServerPlayer.@Nullable RespawnConfig respawnConfig) {
        if (respawnConfig == null) {
            return null;
        }

        LevelData.RespawnData respawnData = SeamRespawnData.insideBounds(
                ((ServerPlayer) (Object) this).level().getServer(), respawnConfig.respawnData());
        return respawnData == respawnConfig.respawnData()
                ? respawnConfig
                : new ServerPlayer.RespawnConfig(respawnData, respawnConfig.forced());
    }

    @WrapMethod(method = "isReachableBedBlock")
    private boolean toroidal$bedReachThroughSeam(BlockPos bedBlockPos, Operation<Boolean> original) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return original.call(bedBlockPos);
        }

        Vec3 bedCenter = Vec3.atBottomCenterOf(bedBlockPos);
        return Math.abs(transformer.coords.x.deltaFromBounds(player.getX(), bedCenter.x())) <= BED_REACH_HORIZONTAL
                && Math.abs(player.getY() - bedCenter.y()) <= BED_REACH_VERTICAL
                && Math.abs(transformer.coords.z.deltaFromBounds(player.getZ(), bedCenter.z())) <= BED_REACH_HORIZONTAL;
    }
}
