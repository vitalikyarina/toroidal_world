package com.toroidalworld.net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.toroidalworld.core.LogRateGate;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.player.ClientPosition;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record TranslationContext(
        WorldFold transformer,
        ClientPosition clientPosition,
        RegistryAccess registryAccess,
        IntFunction<RegistryFriendlyByteBuf> bufferFactory,
        ResourceKey<Level> dimension,
        int trackedViewDistance,
        int heldViewDistance,
        IntPredicate ownVehicle,
        IntFunction<@Nullable Vec3> entityPosition,
        Runnable rebase) {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int VIEW_REACH_SLACK = 2;

    // Vanilla's own floor for a client's requested view distance, below which ChunkMap will not go.
    private static final int MIN_VIEW_DISTANCE = 2;

    // Two chunks: one of lighting border vanilla tracks past the view, one more where it forgets what fell out.
    private static final LogRateGate WARN_GATE = new LogRateGate();

    public static TranslationContext of(ServerPlayer player, WorldFold transformer) {
        int trackedViewDistance = trackedViewDistanceOf(player, transformer);
        return new TranslationContext(
                transformer,
                WorldLoopAttachments.clientPositionOf(player),
                player.registryAccess(),
                Platforms.get().packetBuffers(player),
                player.level().dimension(),
                trackedViewDistance,
                heldViewDistanceOf(player, trackedViewDistance),
                entityId -> isControlledVehicle(player, entityId),
                entityId -> positionOf(player, entityId),
                () -> WorldLoopAttachments.rebaseClientPositionOf(player));
    }

    private static int trackedViewDistanceOf(ServerPlayer player, WorldFold transformer) {
        int serverViewDistance = player.level().getServer().getPlayerList().getViewDistance();
        return transformer.limitViewDistance(
                Mth.clamp(player.requestedViewDistance(), MIN_VIEW_DISTANCE, serverViewDistance));
    }

    private static int heldViewDistanceOf(ServerPlayer player, int trackedViewDistance) {
        return player.getChunkTrackingView() instanceof ChunkTrackingView.Positioned view
                ? view.viewDistance()
                : trackedViewDistance;
    }

    private static boolean isControlledVehicle(ServerPlayer player, int entityId) {
        Entity vehicle = player.getControlledVehicle();
        return vehicle != null && vehicle.getId() == entityId;
    }

    private static @Nullable Vec3 positionOf(ServerPlayer player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        return entity == null ? null : entity.position();
    }

    public ChunkPos toClient(ChunkPos chunkPos, ChunkTraffic traffic) {
        ChunkPos anchor = chunkAnchor();
        ChunkPos clientPos = transformer.nearestCopy(anchor, chunkPos);
        if (isForeign(chunkPos)) {
            return clientPos;
        }

        int viewReach = viewReach();
        int distanceX = Math.abs(clientPos.x - anchor.x);
        int distanceZ = Math.abs(clientPos.z - anchor.z);
        if (distanceX > viewReach || distanceZ > viewReach) {
            warnChunkFarFromAnchor(traffic, chunkPos, clientPos, anchor, viewReach);
        }
        return clientPos;
    }

    private boolean isForeign(ChunkPos chunkPos) {
        return transformer.chunkDomain(Direction.Axis.X).isForeign(chunkPos.x)
                || transformer.chunkDomain(Direction.Axis.Z).isForeign(chunkPos.z);
    }

    private boolean isForeign(Direction.Axis axis, double blockCoord) {
        return transformer.blockDomain(axis).isForeign(blockCoord);
    }

    private ChunkPos chunkAnchor() {
        ChunkPos heldCacheCenter = clientPosition.heldCacheCenter();
        return heldCacheCenter == null ? clientPosition.chunk() : heldCacheCenter;
    }

    public ChunkPos toClientCacheCenter(ChunkPos chunkPos) {
        ChunkPos clientPos = transformer.nearestCopy(clientPosition.chunk(), chunkPos);
        clientPosition.setHeldCacheCenter(clientPos);
        return clientPos;
    }

    public ChunkPos nearestCopy(ChunkPos chunkPos) {
        return transformer.nearestCopy(clientPosition.chunk(), chunkPos);
    }

    public BlockPos nearestCopy(BlockPos pos) {
        return transformer.reseat(pos, nearestCopy(new ChunkPos(pos)));
    }

    public List<ChunkPos> forgetCandidates(ChunkPos chunkPos) {
        ChunkPos anchor = clientPosition.chunk();
        ChunkPos nearest = transformer.nearestCopy(anchor, chunkPos);
        int ambiguityReach = copyAmbiguityReach();
        int[] xCandidates = axisCandidates(
                transformer.chunkDomain(Direction.Axis.X), nearest.x, nearest.x - anchor.x, ambiguityReach);
        int[] zCandidates = axisCandidates(
                transformer.chunkDomain(Direction.Axis.Z), nearest.z, nearest.z - anchor.z, ambiguityReach);

        List<ChunkPos> candidates = new ArrayList<>(xCandidates.length * zCandidates.length);
        for (int xCandidate : xCandidates) {
            for (int zCandidate : zCandidates) {
                candidates.add(new ChunkPos(xCandidate, zCandidate));
            }
        }

        return candidates;
    }

    private static int[] axisCandidates(WrapDomain domain, int nearest, int delta, int ambiguityReach) {
        if (Math.abs(delta) <= ambiguityReach) {
            return new int[] {nearest};
        }

        int other = domain.otherCopy(nearest, delta);
        return other == nearest ? new int[] {nearest} : new int[] {nearest, other};
    }

    private int viewReach() {
        return heldViewDistance + VIEW_REACH_SLACK;
    }

    private int copyAmbiguityReach() {
        return transformer.maxViewDistance() + VIEW_REACH_SLACK;
    }

    public double toClientX(double x, PacketReach reach) {
        double anchor = clientPosition.x();
        double clientX = transformer.blockDomain(Direction.Axis.X).unwrapAround(anchor, x);
        guardReach(reach, Direction.Axis.X, x, clientX, anchor);
        return clientX;
    }

    public double toClientZ(double z, PacketReach reach) {
        double anchor = clientPosition.z();
        double clientZ = transformer.blockDomain(Direction.Axis.Z).unwrapAround(anchor, z);
        guardReach(reach, Direction.Axis.Z, z, clientZ, anchor);
        return clientZ;
    }

    public PacketReach trackedReach() {
        return PacketReach.tracked(trackedViewDistance);
    }

    private void guardReach(PacketReach reach, Direction.Axis axis, double serverValue, double clientValue,
            double anchor) {
        if (!withinReach(clientValue, anchor, reach) && !isForeign(axis, serverValue)) {
            warnCoordFarFromAnchor(reach, axis.getName(), serverValue, clientValue, anchor);
        }
    }

    static boolean withinReach(double clientValue, double anchor, PacketReach reach) {
        return Math.abs(clientValue - anchor) <= reach.blocks() + reach.slackBlocks();
    }

    public double nearestCopyX(double x) {
        return transformer.blockDomain(Direction.Axis.X).unwrapAround(clientPosition.x(), x);
    }

    public double nearestCopyZ(double z) {
        return transformer.blockDomain(Direction.Axis.Z).unwrapAround(clientPosition.z(), z);
    }

    private void warnChunkFarFromAnchor(ChunkTraffic traffic, ChunkPos serverPos, ChunkPos clientPos,
            ChunkPos anchor, int viewReach) {
        if (!WARN_GATE.tryPass()) {
            return;
        }

        LOGGER.warn("A {} chunk lands farther from the client anchor than the view reaches in {}:"
                        + " server {} translated to client {} around anchor {}, view reach {} chunks",
                traffic.key(), dimension.location(), serverPos, clientPos, anchor, viewReach);
    }

    private void warnCoordFarFromAnchor(PacketReach reach, String axis,
            double serverValue, double clientValue, double anchor) {
        if (!WARN_GATE.tryPass()) {
            return;
        }

        LOGGER.warn("A {} packet's {} lands farther from the client anchor than it can reach in {}:"
                        + " server {} translated to client {} around anchor {}, reach {} blocks, slack {} blocks",
                reach.kind(), axis, dimension.location(), serverValue, clientValue, anchor,
                reach.blocks(), reach.slackBlocks());
    }

    public Vec3 toClient(Vec3 position, PacketReach reach) {
        Vec3 clientPos = nearestCopy(position);
        guardReach(reach, Direction.Axis.X, position.x, clientPos.x, clientPosition.x());
        guardReach(reach, Direction.Axis.Z, position.z, clientPos.z, clientPosition.z());
        return clientPos;
    }

    public Vec3 nearestCopy(Vec3 position) {
        return transformer.nearestCopy(new Vec3(clientPosition.x(), position.y, clientPosition.z()), position);
    }

    public BlockPos toServer(BlockPos pos) {
        return transformer.fold(pos);
    }

    public Vec3 toServer(Vec3 position) {
        return transformer.fold(position);
    }
}
