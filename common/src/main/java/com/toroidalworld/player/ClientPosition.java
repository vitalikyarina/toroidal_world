package com.toroidalworld.player;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.toroidalworld.core.LogRateGate;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

// Where the player believes they are. The client is never told the world wraps, so its coordinate keeps growing past
// the seam; the server keeps the wrapped truth and this unwrapped mirror, and translates every packet between them.
// The value is not derived — the client reports it in every movement packet, and we simply remember the last one.
//
// The mirror also remembers which dimension it belongs to. Each wrapped dimension is its own continuous space, so a
// coordinate carried over from the previous one is not stale by a little — it names a place in a different world, and
// unwrapping around it puts the client's chunk cache a whole world from the chunks it is being sent.
public final class ClientPosition {
    private static final Logger LOGGER = LogUtils.getLogger();

    // The two coordinates and the space they belong to are one fact, not three. This is written on the server thread
    // while the packets that read it are translated on the network thread — so a reader catching a rebase halfway would
    // see a coordinate from one world labelled with another, which is worse than either value on its own. Swapping a
    // whole record in a single volatile store leaves nothing to catch halfway. The transformer rides along: it is the
    // shape of the space the mirror describes, seeded with it and replaced with it.
    private record Mirror(double x, double z, @Nullable ResourceKey<Level> space, WorldLoopTransformer transformer) {
    }

    // The world border's centre in client space, as it was last sent. Two loose doubles rather than a position: this is
    // where a square is centred, not a place anything stands, and the border itself keeps it as exactly this pair.
    public record BorderCenter(double x, double z) {
    }

    private volatile Mirror mirror = new Mirror(0.0, 0.0, null, WorldLoopTransformer.NOOP);

    // The copy of the world spawn most recently sent to the client, in client space — the one long-lived absolute
    // coordinate the client stores. Vanilla sends it once and never again, so ClientAnchorSync watches for the moment
    // the nearest copy flips away from this value and re-sends. Written by the packet rewriter on the network thread,
    // read by the tick handler on the server thread; a torn moment costs one redundant re-send, nothing worse.
    private volatile @Nullable BlockPos heldSpawn;

    // The same arrangement for the border's centre, and for the same reason: vanilla sends it on the way into a level
    // and then only when someone moves it, so after a lap the client's square sits a world behind. ClientAnchorSync
    // watches this against the copy the client should hold now.
    private volatile @Nullable BorderCenter heldBorderCenter;

    // The chunk-cache centre most recently sent to the client, in client space — where the client's own chunk cache
    // actually stands, as opposed to the mirror, which runs ahead of it for the tick between a teleport and the
    // tracking view re-centring. Every chunk packet is folded and judged around this, so it is written by the
    // cache-centre rewriter and read by the chunk door, both on the connection's own thread and in packet order.
    private volatile @Nullable ChunkPos heldCacheCenter;

    private final LogRateGate warnGate = new LogRateGate();

    public double x() {
        return seededMirror().x();
    }

    public double z() {
        return seededMirror().z();
    }

    // An unseeded mirror holds (0.0, 0.0) — a plausible spawn-area coordinate, not a recognizable sentinel. Handing it
    // out would anchor packet translation a whole world from the player and corrupt their chunk cache in silence, so a
    // read before the first rebase fails here instead.
    private Mirror seededMirror() {
        Mirror currMirror = this.mirror;
        if (currMirror.space() == null) {
            throw new IllegalStateException("ClientPosition mirror read before the first rebase seeded it");
        }
        return currMirror;
    }

    // X and Z arrive through two separate vanilla clamp sites, so a mover writes them as two stores. A reader between
    // the stores sees the new X with the previous Z — coordinates one movement step apart, in the same space, which the
    // unwrap anchor tolerates. Only the dimension must never tear, and neither store touches it.
    public void setX(double x) {
        Mirror currMirror = this.mirror;
        warnOnHalfWorldStep("x", currMirror.transformer().coords.x, currMirror.x(), x, currMirror.space());
        this.mirror = new Mirror(x, currMirror.z(), currMirror.space(), currMirror.transformer());
    }

    public void setZ(double z) {
        Mirror currMirror = this.mirror;
        warnOnHalfWorldStep("z", currMirror.transformer().coords.z, currMirror.z(), z, currMirror.space());
        this.mirror = new Mirror(currMirror.x(), z, currMirror.space(), currMirror.transformer());
    }

    public void set(double x, double z) {
        Mirror currMirror = this.mirror;
        warnOnHalfWorldStep("x", currMirror.transformer().coords.x, currMirror.x(), x, currMirror.space());
        warnOnHalfWorldStep("z", currMirror.transformer().coords.z, currMirror.z(), z, currMirror.space());
        this.mirror = new Mirror(x, z, currMirror.space(), currMirror.transformer());
    }

    // False until the mirror has been seeded, and false again the moment the player arrives somewhere it was not built
    // for — the one situation where it must be rebased rather than shifted.
    public boolean describes(ResourceKey<Level> dimension) {
        return dimension.equals(this.mirror.space());
    }

    public void rebase(double x, double z, ResourceKey<Level> dimension, WorldLoopTransformer transformer) {
        this.mirror = new Mirror(x, z, dimension, transformer);
        // A new space makes the stored copies meaningless; null makes the refresher send fresh ones.
        this.heldSpawn = null;
        this.heldBorderCenter = null;
        this.heldCacheCenter = null;
    }

    public @Nullable BlockPos heldSpawn() {
        return this.heldSpawn;
    }

    public void setHeldSpawn(BlockPos heldSpawn) {
        this.heldSpawn = heldSpawn;
    }

    public @Nullable BorderCenter heldBorderCenter() {
        return this.heldBorderCenter;
    }

    public void setHeldBorderCenter(BorderCenter heldBorderCenter) {
        this.heldBorderCenter = heldBorderCenter;
    }

    public @Nullable ChunkPos heldCacheCenter() {
        return this.heldCacheCenter;
    }

    public void setHeldCacheCenter(ChunkPos heldCacheCenter) {
        this.heldCacheCenter = heldCacheCenter;
    }

    public ChunkPos chunk() {
        Mirror currMirror = seededMirror();
        return new ChunkPos(
                SectionPos.blockToSectionCoord(currMirror.x()),
                SectionPos.blockToSectionCoord(currMirror.z()));
    }

    // Every mirror move funnels through the setters above except rebase — which is the one legal way to jump it far.
    // So the invariant all of packet translation stands on is checked at its source: a single step longer than half a
    // world flips which copy of a held chunk lies nearest the client, and nothing downstream can tell anymore. The
    // move is still applied — the guard only makes the break loud. Before the first rebase the transformer is NOOP,
    // whose every step fits in half by meaning.
    private void warnOnHalfWorldStep(String axis, WrapDomain domain, double from, double to,
            @Nullable ResourceKey<Level> space) {
        if (domain.fitsInHalf(Math.abs(to - from)) || !warnGate.tryPass()) {
            return;
        }

        Object where = space == null ? "unseeded space" : space.identifier();
        LOGGER.warn("Half-world step invariant violated in {}: mirror {} stepped from {} to {} without a rebase",
                where, axis, from, to);
    }
}
