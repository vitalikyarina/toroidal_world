package com.toroidalworld.mixin;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.net.WrappingBoundsSync;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// A view distance wider than half the world would show the player the same chunk twice — and eventually themselves,
// from behind. The client keeps the distance it asked for; only the server's per-level distance is clamped.
@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Shadow
    @Final
    private List<ServerPlayer> players;

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

    // The first of the three moments the client's space changes and it needs the wrap bounds: joining. The others
    // are crossing to another dimension (ServerPlayerMixin) and the death respawn below — the wrapped dimensions
    // carry different widths. At the tail, once the connection is fully placed and flushing again, so the payload
    // follows the whole login sequence.
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void toroidal$sendBoundsOnLogin(Connection connection, ServerPlayer player, CommonListenerCookie cookie,
            CallbackInfo ci) {
        WrappingBoundsSync.sendTo(player);
    }

    // A death gives the client a brand new player of its own, so the coordinate it was carrying is gone and the mirror
    // has to start again from where the body was placed. Anchored on the placement itself rather than on the position
    // packet that follows: the chunk-cache centre and the chunks are translated around the mirror, and a mirror still
    // holding the pre-death coordinate centres the client a whole world from the chunks it is then sent.
    //
    // The new player is the local, not the argument — the argument is the body that just died, and the listener's own
    // field still points at it until this method returns. Reading the local is also what keeps this off the packet
    // layer, where only the dead player is reachable.
    @Inject(method = "respawn",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;moveTo(DDDFF)V",
                    shift = At.Shift.AFTER))
    private void toroidal$rebaseMirrorOnRespawn(ServerPlayer player, boolean keepEverything,
            Entity.RemovalReason reason, CallbackInfoReturnable<ServerPlayer> cir,
            @Local(ordinal = 1) ServerPlayer respawned) {
        WorldLoopAttachments.rebaseClientPositionOf(respawned);
    }

    @Inject(method = "respawn", at = @At("TAIL"))
    private void toroidal$sendBoundsOnRespawn(ServerPlayer player, boolean keepAllPlayerData,
            Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        WrappingBoundsSync.sendTo(cir.getReturnValue());
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
