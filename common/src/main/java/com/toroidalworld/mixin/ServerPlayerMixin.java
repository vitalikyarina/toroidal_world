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
import com.toroidalworld.net.ClientAnchorSync;
import com.toroidalworld.net.WrappingBoundsSync;
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
    //
    // The bounds come from the dimension the point names, which is a separate argument here and need not be the level
    // the player stands in — a bed slept in before a nether trip is still an overworld coordinate, and it is the
    // overworld's width it has to be folded into.
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

    // The second of the two moments the client's space changes and it needs the wrap bounds: arriving in another
    // dimension (the overworld and the nether wrap at different widths). TAIL lands on the method's last return —
    // the end of the cross-dimension branch; the same-dimension branch and every null bail-out return earlier
    // and change no space, so they are rightly passed by.
    @Inject(method = "changeDimension(Lnet/minecraft/world/level/portal/DimensionTransition;)Lnet/minecraft/world/entity/Entity;",
            at = @At("TAIL"))
    private void toroidal$sendBoundsOnDimensionChange(DimensionTransition transition,
            CallbackInfoReturnable<@Nullable Entity> cir) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        WrappingBoundsSync.sendTo(player);
    }

    // Each server tick, after the player's own vanilla tick has run: the moment the anchors the client holds — the
    // world spawn and the border centre — may need re-sending around the mirror's fresh position. Anchored to the
    // super call rather than doTick's tail so a spectator parked in an unloaded chunk, whose vanilla tick is skipped,
    // skips the refresh with it.
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

    // The client's render distance is the outer bound the tracker gates entity visibility on, and vanilla's writer
    // touches nothing else: the radius every translated coordinate is judged against follows it on the very next
    // packet, while the tracker's standing decision still stands on the radius before it. Re-taking that decision here
    // is what keeps the two from ever naming different numbers.
    @Inject(method = "updateOptions", at = @At("TAIL"))
    private void toroidal$refreshTrackingOnViewChange(ClientInformation information, CallbackInfo ci,
            @Share("oldViewDistance") LocalIntRef oldViewDistance) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        // The constructor writes the options too, before the player has a connection and before anything is tracked
        // for them; there is no standing decision to re-take, and pairing one would send to a connection that is null.
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
