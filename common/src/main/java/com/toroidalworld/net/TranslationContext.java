package com.toroidalworld.net;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.LogRateGate;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.player.ClientPosition;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// Everything a packet rewrite reads from the live server, captured as plain values and functions so the rewriters can
// run without one — a test builds this record by hand where production resolves it from the player.
//
// It also owns the translation between the two coordinate spaces the server juggles: its own wrapped truth, and the
// unbounded space the client believes in. The two directions are not mirror images. Outgoing, a coordinate is unwrapped
// around where the player believes they are — a chunk at the far edge of the world is sent as the chunk just past them,
// which is where they expect it, and which of the infinitely many copies to show depends on where they stand. Incoming,
// a coordinate is simply wrapped back into the world: it names exactly one block, whichever copy the player clicked.
// bufferFactory builds the buffer a rewriter re-encodes a packet through (capacity in bytes), and it has to write the
// wire format the receiving connection itself would use — a modded and a vanilla client do not share one. Which buffer
// class that is belongs to the loader, so the resolver below captures it and the rewriters stay loader-free.
public record TranslationContext(
        WorldLoopTransformer transformer,
        ClientPosition clientPosition,
        RegistryAccess registryAccess,
        IntFunction<RegistryFriendlyByteBuf> bufferFactory,
        ResourceKey<Level> dimension,
        int viewDistance,
        IntPredicate ownVehicle,
        IntFunction<@Nullable Vec3> entityPosition,
        Runnable rebase) {

    private static final Logger LOGGER = LogUtils.getLogger();

    // How far past the capped view distance legitimate chunk traffic still reaches: one chunk of lighting border
    // vanilla tracks beyond the view, and one more where it forgets what fell out.
    private static final int VIEW_REACH_SLACK = 2;

    // The mirror is written from the client's own movement packets, while the radius a packet was gated on was
    // measured against the player's server position a moment earlier. A chunk of blocks covers the gap, so a fast
    // mover is never accused of standing outside a bound they are inside.
    private static final double COORD_REACH_SLACK = CoordinateConstants.CHUNK_WIDTH;

    // Vanilla's own floor for a client's requested view distance, below which ChunkMap will not go.
    private static final int MIN_VIEW_DISTANCE = 2;

    // Shared across players and dimensions — the first line carries everything needed to start digging.
    private static final LogRateGate WARN_GATE = new LogRateGate();

    public static TranslationContext of(ServerPlayer player, WorldLoopTransformer transformer) {
        return new TranslationContext(
                transformer,
                WorldLoopAttachments.clientPositionOf(player),
                player.registryAccess(),
                Platforms.get().packetBuffers(player),
                player.level().dimension(),
                viewDistanceOf(player, transformer),
                entityId -> isControlledVehicle(player, entityId),
                entityId -> positionOf(player, entityId),
                () -> WorldLoopAttachments.rebaseClientPositionOf(player));
    }

    // What the client can actually see, resolved the way ChunkMap resolves it: the client's own request, held to the
    // server's setting and then to the world's ceiling. Not the ceiling on its own — that is half the world by
    // construction, which is exactly the distance a fold can never exceed, so a guard standing on it can never fire.
    private static int viewDistanceOf(ServerPlayer player, WorldLoopTransformer transformer) {
        int serverViewDistance = player.level().getServer().getPlayerList().getViewDistance();
        return transformer.limitViewDistance(
                Mth.clamp(player.requestedViewDistance(), MIN_VIEW_DISTANCE, serverViewDistance));
    }

    private static boolean isControlledVehicle(ServerPlayer player, int entityId) {
        Entity vehicle = player.getControlledVehicle();
        return vehicle != null && vehicle.getId() == entityId;
    }

    private static @Nullable Vec3 positionOf(ServerPlayer player, int entityId) {
        Entity entity = player.level().getEntity(entityId);
        return entity == null ? null : entity.position();
    }

    // Everything anchored to a chunk (light, biomes, auxiliary light, block updates) has to land on the copy the client
    // actually holds. That copy is the plain unwrap around the player: client-space motion only ever advances in steps
    // smaller than half a world (view-edge forgets at r+1 chunks, teleports re-anchored around the current mirror), so
    // the nearest representation of a chunk can never change while the client holds it — verified by a four-lap
    // circumnavigation with a drift probe that never fired, guarded at the source by ClientPosition's own step check,
    // and backstopped here: a chunk farther from the anchor than the view could reach cannot come from view traffic,
    // so it is warned about instead of corrupting the client's cache in silence. No per-chunk memory is needed.
    public ChunkPos toClient(ChunkPos chunkPos) {
        ChunkPos anchor = chunkAnchor();
        ChunkPos clientPos = transformer.chunks.unwrap(anchor, chunkPos);
        int viewReach = viewReach();
        if (Math.abs(clientPos.x() - anchor.x()) > viewReach || Math.abs(clientPos.z() - anchor.z()) > viewReach) {
            warnChunkFarFromAnchor(chunkPos, clientPos, anchor, viewReach);
        }
        return clientPos;
    }

    // Where the client's chunk cache stands, which is not always where the player does. Vanilla gates chunk traffic on
    // the tracking view and announces that view's centre with the cache-centre packet, so the centre the client last
    // received is both the copy its cache is built around and the point the traffic was measured from. The mirror is a
    // different thing: it follows the player, and a teleport moves it a tick before the view re-centres — a window this
    // door used to fold and judge traffic in, from a point the client's cache had not reached. Reading the centre out
    // of the packet stream keeps the two in step by construction: the anchor changes exactly where the client's own
    // cache changes, in the same order, because both are that one packet. Before it has ever arrived — the first
    // chunks of a login or a dimension change — the mirror is the only anchor there is, and it is right, because the
    // client's cache is empty and about to be built around the player.
    private ChunkPos chunkAnchor() {
        ChunkPos heldCacheCenter = clientPosition.heldCacheCenter();
        return heldCacheCenter == null ? clientPosition.chunk() : heldCacheCenter;
    }

    // The cache-centre packet sets the anchor rather than riding on it, so it takes its own door. It folds around the
    // mirror — the centre vanilla computed is the player's own chunk, and the client is about to move its cache there —
    // and it is not held to the view reach, which measures traffic against a centre this packet is still delivering.
    public ChunkPos toClientCacheCenter(ChunkPos chunkPos) {
        ChunkPos clientPos = transformer.chunks.unwrap(clientPosition.chunk(), chunkPos);
        clientPosition.setHeldCacheCenter(clientPos);
        return clientPos;
    }

    // The plain nearest-copy unwrap, outside the view-reach backstop — for packets that name a chunk as a directional
    // hint (a far player's waypoint) rather than a chunk the client holds.
    public ChunkPos nearestCopy(ChunkPos chunkPos) {
        return transformer.chunks.unwrap(clientPosition.chunk(), chunkPos);
    }

    // The copies of a forgotten chunk the client might be holding. Within the view's reach the nearest copy is the
    // held one. Past it the anchor has outrun the coordinate — a multi-chunk view jump — and nothing in the packet
    // says which side the client holds, so both copies of each overrun axis are returned; forgetting the unheld one
    // is a client-side no-op, and the server never tracks both copies at once. An axis that does not wrap has no
    // second copy to offer, and hands back the one coordinate there is.
    public List<ChunkPos> forgetCandidates(ChunkPos chunkPos) {
        ChunkPos anchor = clientPosition.chunk();
        ChunkPos nearest = transformer.chunks.unwrap(anchor, chunkPos);
        int ambiguityReach = copyAmbiguityReach();
        int[] xCandidates = axisCandidates(transformer.chunks.x, nearest.x(), nearest.x() - anchor.x(), ambiguityReach);
        int[] zCandidates = axisCandidates(transformer.chunks.z, nearest.z(), nearest.z() - anchor.z(), ambiguityReach);

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

    // How far a chunk the client is holding may sit from the anchor: as far as its view reaches, and no further.
    private int viewReach() {
        return viewDistance + VIEW_REACH_SLACK;
    }

    // A different question, which the same number used to answer. Which copy of a chunk lies nearest the anchor stops
    // being decidable only at the antipode, so what a forget has to fan out past is half the world — the ceiling the
    // world's shape sets on any view, not the view a particular client asked for. Reading the narrower one here would
    // fan out ordinary forgets that are not ambiguous at all, doubling that traffic for nothing.
    private int copyAmbiguityReach() {
        return transformer.maxViewDistance() + VIEW_REACH_SLACK;
    }

    // The guarded door for a coordinate that reaches the client only because the packet carrying it was offered within
    // some radius of them. That radius is the bound — see PacketReach; the fold itself can produce nothing wider than
    // half the world, so the distance from the anchor says nothing on its own.
    public double toClientX(double x, PacketReach reach) {
        double anchor = clientPosition.x();
        double clientX = transformer.coords.x.unwrapAround(anchor, x);
        guardReach(reach, "x", x, clientX, anchor);
        return clientX;
    }

    public double toClientZ(double z, PacketReach reach) {
        double anchor = clientPosition.z();
        double clientZ = transformer.coords.z.unwrapAround(anchor, z);
        guardReach(reach, "z", z, clientZ, anchor);
        return clientZ;
    }

    // The reach the entity family is bounded by, in blocks — a placement, a teleport, a position sync, a minecart's
    // steps, a damage source, the anchor a particle payload hangs on. All of them ride on a tracked entity, and the
    // tracker never shows one past the client's own view.
    public PacketReach trackedReach() {
        return PacketReach.tracked(viewDistance);
    }

    private void guardReach(PacketReach reach, String axis, double serverValue, double clientValue, double anchor) {
        if (Math.abs(clientValue - anchor) > reach.blocks() + COORD_REACH_SLACK) {
            warnCoordFarFromAnchor(reach, axis, serverValue, clientValue, anchor);
        }
    }

    // The plain nearest-copy fold, outside the guard above — the loose-coordinate twin of nearestCopy(ChunkPos) and of
    // PacketTranslator's nearestCopyBlock. It is the door for a coordinate that reaches the client for a reason other
    // than the player's nearness — a teleport target, the point a look turns toward, a border centre — and so has no
    // radius to be held to at all.
    //
    // Neither door can see a step past the antipode: unwrapAround returns the nearest copy, so the difference it leaves
    // behind is at most half a world by construction. That question belongs to the mirror, where both ends of a move are
    // known, and ClientPosition asks it there.
    public double nearestCopyX(double x) {
        return transformer.coords.x.unwrapAround(clientPosition.x(), x);
    }

    public double nearestCopyZ(double z) {
        return transformer.coords.z.unwrapAround(clientPosition.z(), z);
    }

    // Distinct from ClientPosition's step guard in wording as well as in question: that one measures a move and can
    // catch a real break, these two say a translated coordinate came out farther from the anchor than the traffic
    // carrying it could possibly have reached — which is a break, not a suspicion, and names the radius it broke.
    private void warnChunkFarFromAnchor(ChunkPos serverPos, ChunkPos clientPos, ChunkPos anchor, int viewReach) {
        if (!WARN_GATE.tryPass()) {
            return;
        }

        LOGGER.warn("A chunk lands farther from the client anchor than the view reaches in {}:"
                        + " server {} translated to client {} around anchor {}, view reach {} chunks",
                dimension.identifier(), serverPos, clientPos, anchor, viewReach);
    }

    private void warnCoordFarFromAnchor(PacketReach reach, String axis,
            double serverValue, double clientValue, double anchor) {
        if (!WARN_GATE.tryPass()) {
            return;
        }

        LOGGER.warn("A {} packet's {} lands farther from the client anchor than it can reach in {}:"
                        + " server {} translated to client {} around anchor {}, reach {} blocks",
                reach.kind(), axis, dimension.identifier(), serverValue, clientValue, anchor, reach.blocks());
    }

    public Vec3 toClient(Vec3 position, PacketReach reach) {
        return new Vec3(toClientX(position.x, reach), position.y, toClientZ(position.z, reach));
    }

    public BlockPos toServer(BlockPos pos) {
        return transformer.blocks.wrap(pos);
    }

    public Vec3 toServer(Vec3 position) {
        return transformer.vectors.wrap(position);
    }
}
