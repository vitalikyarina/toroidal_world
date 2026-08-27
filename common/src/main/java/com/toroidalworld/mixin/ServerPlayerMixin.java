package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.accessors.TrackedEntityRefresher;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.entity.SeamAim;
import com.toroidalworld.net.ClientAnchorSync;
import com.toroidalworld.net.WorldShapeSync;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.toroidalworld.storage.SeamRespawnData;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @ModifyVariable(method = "setRespawnPosition(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;FZZ)V",
            at = @At("HEAD"), argsOnly = true)
    private @Nullable BlockPos toroidal$storeRespawnInsideBounds(@Nullable BlockPos respawnPos,
            @Local(argsOnly = true) ResourceKey<Level> respawnDimension) {
        if (respawnPos == null) {
            return null;
        }

        MinecraftServer server = ((ServerPlayer) (Object) this).getServer();
        ServerLevel level = server == null ? null : server.getLevel(respawnDimension);
        if (level == null) {
            return respawnPos;
        }

        return SeamRespawnData.insideBounds(level, respawnPos);
    }

    @ModifyVariable(method = "indicateDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double toroidal$hurtDirX(double xd) {
        return SeamAim.foldX((ServerPlayer) (Object) this, xd);
    }

    @ModifyVariable(method = "indicateDamage", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double toroidal$hurtDirZ(double zd) {
        return SeamAim.foldZ((ServerPlayer) (Object) this, zd);
    }

    @Inject(method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;",
            at = @At("TAIL"))
    private void toroidal$sendBoundsOnDimensionChange(DimensionTransition transition,
            CallbackInfoReturnable<@Nullable Entity> cir) {
        WorldShapeSync.sendTo((ServerPlayer) (Object) this);
    }

    @Inject(method = "doTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;tick()V",
                    shift = At.Shift.AFTER))
    private void toroidal$refreshClientAnchors(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        ClientAnchorSync.refresh(player);
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

        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return;
        }

        TrackedEntityRefresher refresher =
                (TrackedEntityRefresher) (Object) player.serverLevel().getChunkSource().chunkMap;
        refresher.toroidal$refreshTrackedEntities(player);
    }

    @WrapMethod(method = "isReachableBedBlock")
    private boolean toroidal$bedReachThroughSeam(BlockPos bedBlockPos, Operation<Boolean> original) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(player.level());
        if (transformer == null) {
            return original.call(bedBlockPos);
        }

        return original.call(transformer.nearestCopy(player.blockPosition(), bedBlockPos));
    }
}
