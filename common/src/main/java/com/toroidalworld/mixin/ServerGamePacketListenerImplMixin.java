package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.ChunkResender;
import com.toroidalworld.accessors.ClientPositionHolder;
import com.toroidalworld.accessors.TrackedEntityRefresher;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.player.ClientPosition;
import com.toroidalworld.player.MirrorWriter;
import com.toroidalworld.player.SeamSnap;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

// The player moves in the unbounded space the client believes in; the server keeps them inside the world. Each movement
// packet is therefore read twice: once as the client's own coordinate (remembered), once as the nearest continuous
// position around the player (so vanilla's distance checks still see a normal step, not a jump across the world).
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin implements ClientPositionHolder {
    @Shadow
    public ServerPlayer player;

    // Keep this the last field with an initialiser: mixin splices declaration initialisers by line-number range.
    @Unique
    private final ClientPosition toroidal$clientPosition = new ClientPosition();

    @Override
    public ClientPosition toroidal$clientPosition() {
        return this.toroidal$clientPosition;
    }

    // Seeded here so the field can be final: the getter is reached from the server thread and the network thread alike.
    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$seedMirror(MinecraftServer server, Connection connection, ServerPlayer player,
            CommonListenerCookie cookie, CallbackInfo ci) {
        WorldLoopAttachments.rebaseClientPositionOf(player);
    }

    // The use-on ack names the clicked block's neighbour, and vanilla steps to it with a plain BlockPos.relative. The
    // clicked block itself arrives wrapped — the inbound rewriter saw to that — so the step lands outside the world
    // exactly when the block is the last one on its axis and the hit face points outward, which is the face reachable
    // only from across the seam. Wrapped here, at the step that produces it, rather than where it is read: the same
    // coordinate feeds the packet the client is sent, and a server truth outside the bounds has no other owner.
    @ModifyExpressionValue(
            method = "handleUseItemOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;relative(Lnet/minecraft/core/Direction;)Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$wrapAckedNeighbour(BlockPos neighbour) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        return transformer == null ? neighbour : transformer.blocks.wrap(neighbour);
    }

    // Every player teleport funnels through here, so this is also where the server's own truth is kept inside the
    // world. A cross-seam portal exit is computed in unwrapped space and can name a position past the bounds; placed
    // there, the player stands in a phantom chunk until the next move packet wraps them back — and in that window the
    // arrival packet (sent raw) and the mirror seed (wrapped) disagree by a whole world width, so the chunk-cache
    // centre and the chunks go to a frame the client is not in, and the post-teleport screen holds for its full 30 s.
    // Wrapped here, before the entity is placed and the packet is built, the entity, the packet and the
    // mirror describe the same in-bounds place from the first packet of the new dimension. On a non-wrapped level
    // wrap() is the identity.
    //
    // Both axes are wrapped, with no relative case to skip: this version's teleport takes absolute arguments whatever
    // the relative set says, and hands them straight to absMoveTo — the set only decides what the packet carries, and
    // the delta for it is computed here from these same arguments. That leaves a relative axis a wire delta a whole
    // world wide whenever the wrap moved the value; the position rewriter folds it modulo the width, back to the step
    // the client would have received untouched.
    //
    // The ordinals count the double arguments, not the slots: x is the first, z the third.
    @ModifyVariable(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double toroidal$wrapTeleportX(double x) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return x;
        }

        return transformer.coords.x.wrap(x);
    }

    @ModifyVariable(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double toroidal$wrapTeleportZ(double z) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return z;
        }

        return transformer.coords.z.wrap(z);
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
    private void toroidal$dropChunksBeforeTeleport(double x, double y, double z, float yRot, float xRot,
            Set<RelativeMovement> relatives, CallbackInfo ci,
            @Share("stormWholeView") LocalBooleanRef stormWholeView,
            @Share("flippedChunks") LocalRef<List<ChunkPos>> flippedChunks) {
        ChunkResender resender = toroidal$chunkResender();
        if (resender == null) {
            return;
        }

        ClientPosition mirror = WorldLoopAttachments.clientPositionOf(this.player);
        if (!mirror.describes(this.player.level().dimension())) {
            stormWholeView.set(true);
            resender.toroidal$dropTrackedChunks(this.player);
            return;
        }

        List<ChunkPos> flipped = toroidal$flippedChunks(x, z, mirror);
        flippedChunks.set(flipped);
        if (!flipped.isEmpty()) {
            resender.toroidal$dropChunks(this.player, flipped);
        }
    }

    @Inject(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("TAIL"))
    private void toroidal$resendChunksAfterTeleport(double x, double y, double z, float yRot, float xRot,
            Set<RelativeMovement> relatives, CallbackInfo ci,
            @Share("stormWholeView") LocalBooleanRef stormWholeView,
            @Share("flippedChunks") LocalRef<List<ChunkPos>> flippedChunks) {
        ChunkResender resender = toroidal$chunkResender();
        if (resender == null) {
            return;
        }

        if (stormWholeView.get()) {
            resender.toroidal$resendTrackedChunks(this.player);
        } else {
            List<ChunkPos> flipped = flippedChunks.get();
            if (flipped != null && !flipped.isEmpty()) {
                resender.toroidal$resendChunks(this.player, flipped);
            }
        }

        toroidal$refreshTrackedEntities();
    }

    @Unique
    private void toroidal$refreshTrackedEntities() {
        TrackedEntityRefresher refresher =
                (TrackedEntityRefresher) (Object) this.player.serverLevel().getChunkSource().chunkMap;
        refresher.toroidal$refreshTrackedEntities(this.player);
    }

    // Which held chunks the mirror's jump moves to a different client-space copy. The destination is absolute on every
    // axis here, so the predicted landing is the nearest copy of it around the mirror, with no relative case to
    // predict separately — for a relative axis the client applies the packet's delta to its own coordinate, and that
    // lands on the same chunk: the mirror and the server position describe one physical place, so mirror plus folded
    // delta and nearest-copy-of-destination differ by no whole world. unwrapAround counts whole laps off the
    // difference between its two arguments, so a destination a world out folds to the same copy as one already inside
    // the bounds — which is what lets this run at HEAD without caring whether the wrap hook above has gone first.
    //
    // Each view position is folded to its physical chunk before comparing (the view square may run past the bounds)
    // and compared through the same unwrap the packet translator applies, so the verdict matches what the client would
    // actually be sent; the collected list keeps the raw view coordinate, the same one vanilla's own view difference
    // feeds to its forget and send.
    @Unique
    private List<ChunkPos> toroidal$flippedChunks(double destinationX, double destinationZ, ClientPosition mirror) {
        WorldLoopTransformer transformer = WorldLoopAttachments.transformerOf(this.player.level());
        double clientX = transformer.coords.x.unwrapAround(mirror.x(), destinationX);
        double clientZ = transformer.coords.z.unwrapAround(mirror.z(), destinationZ);

        ChunkPos fromAnchor = mirror.chunk();
        ChunkPos toAnchor = new ChunkPos(
                SectionPos.blockToSectionCoord(clientX),
                SectionPos.blockToSectionCoord(clientZ));

        List<ChunkPos> flipped = new ArrayList<>();
        this.player.getChunkTrackingView().forEach(viewPos -> {
            ChunkPos physical = transformer.chunks.wrap(viewPos);
            if (!transformer.chunks.unwrap(fromAnchor, physical).equals(transformer.chunks.unwrap(toAnchor, physical))) {
                flipped.add(viewPos);
            }
        });
        return flipped;
    }

    @Unique
    private @Nullable ChunkResender toroidal$chunkResender() {
        if (WorldLoopAttachments.wrappedTransformerOf(this.player.level()) == null) {
            return null;
        }

        return (ChunkResender) (Object) this.player.serverLevel().getChunkSource().chunkMap;
    }

    @Shadow
    private double firstGoodX;

    @Shadow
    private double firstGoodZ;

    @Shadow
    private double lastGoodX;

    @Shadow
    private double lastGoodZ;

    @Shadow
    private double vehicleFirstGoodX;

    @Shadow
    private double vehicleFirstGoodZ;

    @Shadow
    private double vehicleLastGoodX;

    @Shadow
    private double vehicleLastGoodZ;

    @WrapOperation(
            method = "handleMovePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D",
                    ordinal = 0))
    private double toroidal$continuousX(double clientX, Operation<Double> original,
            @Local(argsOnly = true) ServerboundMovePlayerPacket packet) {
        double clamped = original.call(clientX);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null || !packet.hasPosition()) {
            return clamped;
        }

        // The mirror lives on this very listener — this.player.connection is this — so the move path reads the field
        // instead of routing through the holder cast, once per axis per packet.
        ClientPosition mirror = this.toroidal$clientPosition;
        mirror.setX(clamped, MirrorWriter.PLAYER_MOVE);
        double unwrapped = transformer.coords.x.unwrapAround(this.player.getX(), clamped);

        this.firstGoodX = transformer.coords.x.unwrapAround(unwrapped, this.firstGoodX);
        this.lastGoodX = transformer.coords.x.unwrapAround(unwrapped, this.lastGoodX);
        return unwrapped;
    }

    @WrapOperation(
            method = "handleMovePlayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D",
                    ordinal = 1))
    private double toroidal$continuousZ(double clientZ, Operation<Double> original,
            @Local(argsOnly = true) ServerboundMovePlayerPacket packet) {
        double clamped = original.call(clientZ);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null || !packet.hasPosition()) {
            return clamped;
        }

        ClientPosition mirror = this.toroidal$clientPosition;
        mirror.setZ(clamped, MirrorWriter.PLAYER_MOVE);
        double unwrapped = transformer.coords.z.unwrapAround(this.player.getZ(), clamped);

        // Same fold as X: the reference the check measures from must name the copy the player is actually standing in.
        this.firstGoodZ = transformer.coords.z.unwrapAround(unwrapped, this.firstGoodZ);
        this.lastGoodZ = transformer.coords.z.unwrapAround(unwrapped, this.lastGoodZ);
        return unwrapped;
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void toroidal$wrapIntoBounds(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null || !transformer.vectors.isOver(this.player.position())) {
            return;
        }

        Vec3 wrapped = transformer.vectors.wrap(this.player.position());
        this.player.absMoveTo(wrapped.x, wrapped.y, wrapped.z, this.player.getYRot(), this.player.getXRot());
        this.firstGoodX = wrapped.x;
        this.firstGoodZ = wrapped.z;
        this.lastGoodX = wrapped.x;
        this.lastGoodZ = wrapped.z;
        this.player.serverLevel().getChunkSource().move(this.player);
    }

    @WrapOperation(
            method = "handleMoveVehicle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D",
                    ordinal = 0))
    private double toroidal$vehicleContinuousX(double clientX, Operation<Double> original) {
        double clamped = original.call(clientX);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return clamped;
        }

        WorldLoopAttachments.clientPositionOf(this.player).setX(clamped, MirrorWriter.VEHICLE_MOVE);
        return transformer.coords.x.unwrapAround(this.player.getRootVehicle().getX(), clamped);
    }

    @WrapOperation(
            method = "handleMoveVehicle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D",
                    ordinal = 1))
    private double toroidal$vehicleContinuousZ(double clientZ, Operation<Double> original) {
        double clamped = original.call(clientZ);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return clamped;
        }

        WorldLoopAttachments.clientPositionOf(this.player).setZ(clamped, MirrorWriter.VEHICLE_MOVE);
        return transformer.coords.z.unwrapAround(this.player.getRootVehicle().getZ(), clamped);
    }

    @Inject(method = "handleMoveVehicle", at = @At("RETURN"))
    private void toroidal$wrapVehicleIntoBounds(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return;
        }

        Entity vehicle = this.player.getRootVehicle();
        if (vehicle == this.player || !transformer.vectors.isOver(vehicle.position())) {
            return;
        }

        Vec3 wrapped = transformer.vectors.wrap(vehicle.position());
        SeamSnap.withPassengers(vehicle, wrapped.subtract(vehicle.position()));

        this.vehicleFirstGoodX = vehicle.getX();
        this.vehicleFirstGoodZ = vehicle.getZ();
        this.vehicleLastGoodX = vehicle.getX();
        this.vehicleLastGoodZ = vehicle.getZ();
    }
}
