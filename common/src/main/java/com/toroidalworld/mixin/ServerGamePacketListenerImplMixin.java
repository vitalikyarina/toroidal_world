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
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.net.PacketProbe;
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

    // Keep this the last field with an initialiser in the file. The declaration initialiser is spliced into the target
    // constructor by a line-number range, and an initialised field declared on a later line — even a static one —
    // stretches that range over this line, so the splice is silently dropped and the mirror sits null on the network
    // thread.
    @Unique
    private final ClientPosition toroidal$clientPosition = new ClientPosition();

    @Override
    public ClientPosition toroidal$clientPosition() {
        return this.toroidal$clientPosition;
    }

    // Seeded here rather than on first use so the field can be final. Created lazily it was a data race: the getter is
    // reached from the server thread while handling movement and from the network thread while translating packets, and
    // two threads finding it null each built their own mirror — after which the movement writes went to an object the
    // packet translator no longer read, and the mirror sat frozen at the login position.
    //
    // The connection is the right moment: the client has just been told where it is, so the two spaces still agree, and
    // from then on the client reports its own coordinate in every movement packet.
    @Inject(method = "<init>", at = @At("TAIL"))
    private void toroidal$seedMirror(MinecraftServer server, Connection connection, ServerPlayer player,
            CommonListenerCookie cookie, CallbackInfo ci) {
        WorldLoopAttachments.rebaseClientPositionOf(player);
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

        double wrapped = transformer.coords.x.wrap(x);
        PacketProbe.teleportWrap(this.player.level().dimension(), "x", x, wrapped);
        return wrapped;
    }

    @ModifyVariable(method = "teleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private double toroidal$wrapTeleportZ(double z) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(this.player.level());
        if (transformer == null) {
            return z;
        }

        double wrapped = transformer.coords.z.wrap(z);
        PacketProbe.teleportWrap(this.player.level().dimension(), "z", z, wrapped);
        return wrapped;
    }

    // Every player teleport funnels through here (commands, pearls, portals resolve to this), and it jumps the mirror
    // past the one-step increment ordinary movement keeps to. The chunk→client mapping is a pure function of the mirror,
    // so a chunk the client still holds can now map to a different copy: vanilla neither re-sends it at the new copy
    // (void behind the player) nor forgets it at the old one (a ghost a world away). The fix straddles the mirror move.
    //
    // Both halves are needed only for the chunks whose client-space copy the jump actually changes — a teleport inside
    // the current frame changes none, and forgetting the whole view for it would throw away meshes the client could
    // keep. The set is computed once, HEAD, before the mirror moves, and shared with TAIL through the invocation-scoped
    // local: computing it twice could disagree across the mirror move and leave chunks dropped but never re-sent.
    //
    // HEAD, before the mirror shifts: forget the flipped chunks, so the forgets name the copies the client is actually
    // holding. The position packet this method sends next moves the mirror; the re-send below then goes out around it.
    // A mirror built for another dimension has nothing to diff against — there the whole view is dropped and re-sent.
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
            PacketProbe.teleportChunks(this.player.level().dimension(), true, 0);
            resender.toroidal$dropTrackedChunks(this.player);
            return;
        }

        List<ChunkPos> flipped = toroidal$flippedChunks(x, z, mirror);
        PacketProbe.teleportChunks(this.player.level().dimension(), false, flipped.size());
        flippedChunks.set(flipped);
        if (!flipped.isEmpty()) {
            resender.toroidal$dropChunks(this.player, flipped);
        }
    }

    // TAIL, mirror now moved by the position packet above: re-send the same chunks, so each lands on the copy the
    // client should now hold. The chunks flush over later ticks, after that position packet, so they translate right.
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
            return;
        }

        List<ChunkPos> flipped = flippedChunks.get();
        if (flipped != null && !flipped.isEmpty()) {
            resender.toroidal$resendChunks(this.player, flipped);
        }
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
        mirror.setX(clamped);
        double unwrapped = transformer.coords.x.unwrapAround(this.player.getX(), clamped);

        // The movement check measures targetX - firstGoodX, a distance taken in raw coordinates, and the bounds it
        // measures from are only refreshed when the client acknowledges a teleport. A portal exit maps to the far side
        // of the world, so between the arrival and that acknowledgement the reference names the same physical place a
        // whole world away: every packet then reads as a 512-block jump, is rejected, and the player is pulled back to
        // the portal they just left. Folding the reference to its nearest copy is the same treatment every other
        // distance in the mod already gets — and it is the identity wherever the seam is not involved, so ordinary
        // movement keeps vanilla's check byte-for-byte.
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
        mirror.setZ(clamped);
        double unwrapped = transformer.coords.z.unwrapAround(this.player.getZ(), clamped);

        // Same fold as X: the reference the check measures from must name the copy the player is actually standing in.
        this.firstGoodZ = transformer.coords.z.unwrapAround(unwrapped, this.firstGoodZ);
        this.lastGoodZ = transformer.coords.z.unwrapAround(unwrapped, this.lastGoodZ);
        return unwrapped;
    }

    // The step above lets the player walk out of the world by up to one move; here they are brought back to the other
    // side. absMoveTo also resets the old position, so nothing interpolates across the whole world, and the movement
    // bounds have to follow — they are the reference the next packet's distance checks measure against.
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

    // A ridden vehicle moves the same way, but through its own packet, and the reference to keep the movement continuous
    // is the vehicle's position, not the player's. This is also where the client position is kept fed — the player rides
    // the vehicle and sends no move packets of their own, so otherwise the chunk cache stops following the boat and the
    // world stops loading around it.
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

        WorldLoopAttachments.clientPositionOf(this.player).setX(clamped);
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

        WorldLoopAttachments.clientPositionOf(this.player).setZ(clamped);
        return transformer.coords.z.unwrapAround(this.player.getRootVehicle().getZ(), clamped);
    }

    // The vehicle's position is applied packet-by-packet, so it must come back inside the world here too, not only at
    // the vehicle's own tick end: out of bounds it stands in a phantom chunk, which never ticks — the tick-end wrap
    // then never runs, and the boat with its rider is stranded outside the world, deaf to dismount input. The whole
    // passenger stack shifts together, same as the tick-end path.
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
    }
}
