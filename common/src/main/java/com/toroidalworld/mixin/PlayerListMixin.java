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

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.net.ListenerCopies;
import com.toroidalworld.net.WorldShapeSync;
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
import net.minecraft.world.phys.Vec3;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Shadow
    @Final
    private List<ServerPlayer> players;

    @WrapMethod(method = "broadcast")
    private void toroidal$broadcastThroughSeam(@Nullable Player except, double x, double y, double z, double range,
            ResourceKey<Level> dimension, Packet<?> packet, Operation<Void> original) {
        WorldFold transformer = null;
        for (ServerPlayer player : this.players) {
            if (player != except && player.level().dimension() == dimension) {
                transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
                break;
            }
        }

        if (transformer == null) {
            original.call(except, x, y, z, range, dimension, packet);
            return;
        }

        Vec3 source = new Vec3(x, y, z);
        List<Vec3> copies = ListenerCopies.nearestTo(transformer, this.players,
                player -> player != except && player.level().dimension() == dimension, source);
        for (Vec3 copy : copies) {
            original.call(except, copy.x, copy.y, copy.z, range, dimension, packet);
        }
    }

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void toroidal$sendBoundsOnLogin(Connection connection, ServerPlayer player, CommonListenerCookie cookie,
            CallbackInfo ci) {
        WorldShapeSync.sendTo(player);
    }

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
        WorldShapeSync.sendTo(cir.getReturnValue());
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
