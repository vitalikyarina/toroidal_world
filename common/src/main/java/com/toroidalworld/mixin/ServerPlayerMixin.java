package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.accessors.TrackedEntityRefresher;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamAim;
import com.toroidalworld.net.ClientAnchorSync;
import com.toroidalworld.net.WorldShapeSync;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.toroidalworld.storage.SeamRespawnData;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Unique
    private static final double BED_REACH_HORIZONTAL = 3.0;

    @Unique
    private static final double BED_REACH_VERTICAL = 2.0;

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

    @ModifyVariable(method = "indicateDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double toroidal$hurtDirX(double xd) {
        return SeamAim.foldX((ServerPlayer) (Object) this, xd);
    }

    @ModifyVariable(method = "indicateDamage", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double toroidal$hurtDirZ(double zd) {
        return SeamAim.foldZ((ServerPlayer) (Object) this, zd);
    }

    @Inject(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;",
            at = @At("TAIL"))
    private void toroidal$sendBoundsOnDimensionChange(TeleportTransition transition,
            CallbackInfoReturnable<@Nullable ServerPlayer> cir) {
        WorldShapeSync.sendTo((ServerPlayer) (Object) this);
    }

    @Inject(method = "doTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;tick()V",
                    shift = At.Shift.AFTER))
    private void toroidal$refreshClientAnchors(CallbackInfo ci) {
        ClientAnchorSync.refresh((ServerPlayer) (Object) this);
    }

    @Inject(method = "updateOptions", at = @At("HEAD"))
    private void toroidal$captureRequestedViewDistance(ClientInformation information, CallbackInfo ci,
            @Share("oldViewDistance") LocalIntRef oldViewDistance) {
        oldViewDistance.set(((ServerPlayer) (Object) this).requestedViewDistance());
    }

    @Inject(method = "updateOptions", at = @At("TAIL"))
    private void toroidal$refreshTrackingOnViewChange(ClientInformation information, CallbackInfo ci,
            @Share("oldViewDistance") LocalIntRef oldViewDistance) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.connection == null || oldViewDistance.get() == player.requestedViewDistance()) {
            return;
        }

        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return;
        }

        TrackedEntityRefresher refresher =
                (TrackedEntityRefresher) (Object) player.level().getChunkSource().chunkMap;
        refresher.toroidal$refreshTrackedEntities(player);
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
