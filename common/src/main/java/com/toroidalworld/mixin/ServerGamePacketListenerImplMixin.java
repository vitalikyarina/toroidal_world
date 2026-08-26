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
import com.toroidalworld.core.SeamDelta;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.player.ClientPosition;
import com.toroidalworld.player.SeamSnap;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

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

    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$seedMirror(MinecraftServer server, Connection connection, ServerPlayer player,
            CommonListenerCookie cookie, CallbackInfo ci) {
        WorldLoopAttachments.rebaseClientPositionOf(player);
    }

    @ModifyVariable(
            method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private PositionMoveRotation toroidal$wrapTeleportDestination(PositionMoveRotation destination,
            @Local(argsOnly = true) Set<Relative> relatives) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return destination;
        }

        Vec3 position = destination.position();
        double wrappedX = relatives.contains(Relative.X)
                ? position.x
                : transformer.blockDomain(Direction.Axis.X).wrap(position.x);
        double wrappedZ = relatives.contains(Relative.Z)
                ? position.z
                : transformer.blockDomain(Direction.Axis.Z).wrap(position.z);
        if (wrappedX == position.x && wrappedZ == position.z) {
            return destination;
        }

        return new PositionMoveRotation(
                new Vec3(wrappedX, position.y, wrappedZ),
                destination.deltaMovement(), destination.yRot(), destination.xRot());
    }

    @Inject(method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V", at = @At("HEAD"))
    private void toroidal$dropChunksBeforeTeleport(PositionMoveRotation destination, Set<Relative> relatives, CallbackInfo ci,
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

        List<ChunkPos> flipped = toroidal$flippedChunks(destination, relatives, mirror);
        flippedChunks.set(flipped);
        if (!flipped.isEmpty()) {
            resender.toroidal$dropChunks(this.player, flipped);
        }
    }

    @Inject(method = "teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V", at = @At("TAIL"))
    private void toroidal$resendChunksAfterTeleport(PositionMoveRotation destination, Set<Relative> relatives, CallbackInfo ci,
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
                (TrackedEntityRefresher) (Object) this.player.level().getChunkSource().chunkMap;
        refresher.toroidal$refreshTrackedEntities(this.player);
    }

    @Unique
    private List<ChunkPos> toroidal$flippedChunks(PositionMoveRotation destination, Set<Relative> relatives, ClientPosition mirror) {
        WorldFold transformer = WorldLoopAttachments.transformerOf(this.player.level());
        Vec3 position = destination.position();
        double clientX = relatives.contains(Relative.X)
                ? mirror.x() + SeamDelta.foldX(transformer, position.x)
                : transformer.blockDomain(Direction.Axis.X).unwrapAround(mirror.x(), position.x);
        double clientZ = relatives.contains(Relative.Z)
                ? mirror.z() + SeamDelta.foldZ(transformer, position.z)
                : transformer.blockDomain(Direction.Axis.Z).unwrapAround(mirror.z(), position.z);

        ChunkPos fromAnchor = mirror.chunk();
        ChunkPos toAnchor = new ChunkPos(
                SectionPos.blockToSectionCoord(clientX),
                SectionPos.blockToSectionCoord(clientZ));

        List<ChunkPos> flipped = new ArrayList<>();
        this.player.getChunkTrackingView().forEach(viewPos -> {
            ChunkPos physical = transformer.fold(viewPos);
            if (!transformer.nearestCopy(fromAnchor, physical).equals(transformer.nearestCopy(toAnchor, physical))) {
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

        return (ChunkResender) (Object) this.player.level().getChunkSource().chunkMap;
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
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null || !packet.hasPosition()) {
            return clamped;
        }

        ClientPosition mirror = this.toroidal$clientPosition;
        mirror.setX(clamped);
        double unwrapped = transformer.blockDomain(Direction.Axis.X).unwrapAround(this.player.getX(), clamped);

        this.firstGoodX = transformer.blockDomain(Direction.Axis.X).unwrapAround(unwrapped, this.firstGoodX);
        this.lastGoodX = transformer.blockDomain(Direction.Axis.X).unwrapAround(unwrapped, this.lastGoodX);
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
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null || !packet.hasPosition()) {
            return clamped;
        }

        ClientPosition mirror = this.toroidal$clientPosition;
        mirror.setZ(clamped);
        double unwrapped = transformer.blockDomain(Direction.Axis.Z).unwrapAround(this.player.getZ(), clamped);

        this.firstGoodZ = transformer.blockDomain(Direction.Axis.Z).unwrapAround(unwrapped, this.firstGoodZ);
        this.lastGoodZ = transformer.blockDomain(Direction.Axis.Z).unwrapAround(unwrapped, this.lastGoodZ);
        return unwrapped;
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void toroidal$wrapIntoBounds(ServerboundMovePlayerPacket packet, CallbackInfo ci) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null || !transformer.isOver(this.player.position())) {
            return;
        }

        Vec3 wrapped = transformer.fold(this.player.position());
        SeamSnap.withPassengers(this.player, wrapped.subtract(this.player.position()));
        this.firstGoodX = wrapped.x;
        this.firstGoodZ = wrapped.z;
        this.lastGoodX = wrapped.x;
        this.lastGoodZ = wrapped.z;
    }

    @WrapOperation(
            method = "handleMoveVehicle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D",
                    ordinal = 0))
    private double toroidal$vehicleContinuousX(double clientX, Operation<Double> original) {
        double clamped = original.call(clientX);
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return clamped;
        }

        WorldLoopAttachments.clientPositionOf(this.player).setX(clamped);
        return transformer.blockDomain(Direction.Axis.X).unwrapAround(this.player.getRootVehicle().getX(), clamped);
    }

    @WrapOperation(
            method = "handleMoveVehicle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clampHorizontal(D)D",
                    ordinal = 1))
    private double toroidal$vehicleContinuousZ(double clientZ, Operation<Double> original) {
        double clamped = original.call(clientZ);
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return clamped;
        }

        WorldLoopAttachments.clientPositionOf(this.player).setZ(clamped);
        return transformer.blockDomain(Direction.Axis.Z).unwrapAround(this.player.getRootVehicle().getZ(), clamped);
    }

    @Inject(method = "handleMoveVehicle", at = @At("RETURN"))
    private void toroidal$wrapVehicleIntoBounds(ServerboundMoveVehiclePacket packet, CallbackInfo ci) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return;
        }

        Entity vehicle = this.player.getRootVehicle();
        if (vehicle == this.player || !transformer.isOver(vehicle.position())) {
            return;
        }

        Vec3 wrapped = transformer.fold(vehicle.position());
        SeamSnap.withPassengers(vehicle, wrapped.subtract(vehicle.position()));

        this.vehicleFirstGoodX = vehicle.getX();
        this.vehicleFirstGoodZ = vehicle.getZ();
        this.vehicleLastGoodX = vehicle.getX();
        this.vehicleLastGoodZ = vehicle.getZ();
    }
}
