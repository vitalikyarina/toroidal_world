package com.toroidalworld.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// A view distance wider than half the world would show the player the same chunk twice — and eventually themselves,
// from behind. The client keeps the distance it asked for; only the server's per-level distance is clamped.
@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Shadow
    @Final
    private List<ServerPlayer> players;

    // Everything positional the world says out loud — sounds, level events — is broadcast to whoever is close enough.
    // Across the seam the plain distance is a whole world, so a player standing right there would be told nothing.
    //
    // Vanilla-body re-implementation — verified against 26.2; re-diff on a platform bump.
    @WrapMethod(method = "broadcast")
    private void toroidal$broadcastThroughSeam(@Nullable Player except, double x, double y, double z, double range,
            ResourceKey<Level> dimension, Packet<?> packet, Operation<Void> original) {
        for (ServerPlayer player : this.players) {
            if (player == except || player.level().dimension() != dimension) {
                continue;
            }

            WorldLoopTransformer transformer = WorldLoopAttachments.transformerOf(player.level());
            double distanceSqr = transformer.isWrapped()
                    ? transformer.coords.sqrDistToBounds(player.getX(), player.getY(), player.getZ(), x, y, z)
                    : player.distanceToSqr(x, y, z);

            if (distanceSqr < range * range) {
                player.connection.send(packet);
            }
        }
    }

    @WrapOperation(
            method = "setViewDistance",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;setViewDistance(I)V"))
    private void toroidal$limitViewDistance(ServerChunkCache chunkSource, int viewDistance, Operation<Void> original,
            @Local ServerLevel level) {
        original.call(chunkSource, WorldLoopAttachments.transformerOf(level).limitViewDistance(viewDistance));
    }

    @WrapOperation(
            method = "setSimulationDistance",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;setSimulationDistance(I)V"))
    private void toroidal$limitSimulationDistance(ServerChunkCache chunkSource, int simulationDistance, Operation<Void> original,
            @Local ServerLevel level) {
        original.call(chunkSource, WorldLoopAttachments.transformerOf(level).limitViewDistance(simulationDistance));
    }
}
