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

    // One record because the two coordinates and their space are one fact: written on the server thread, read on the network thread.
    private volatile @Nullable ChunkPos heldCacheCenter;

    private final LogRateGate warnGate = new LogRateGate();

    public double x() {
        return seededMirror().x();
    }

    public double z() {
        return seededMirror().z();
    }

    private Mirror seededMirror() {
        Mirror currMirror = this.mirror;
        if (currMirror.space() == null) {
            throw new IllegalStateException("ClientPosition mirror read before the first rebase seeded it");
        }
        return currMirror;
    }

    public void setX(double x, MirrorWriter writer) {
        Mirror currMirror = this.mirror;
        checkStep(writer, "x", currMirror.transformer().coords.x, currMirror.x(), x, currMirror.space());
        this.mirror = new Mirror(x, currMirror.z(), currMirror.space(), currMirror.transformer());
    }

    public void setZ(double z, MirrorWriter writer) {
        Mirror currMirror = this.mirror;
        checkStep(writer, "z", currMirror.transformer().coords.z, currMirror.z(), z, currMirror.space());
        this.mirror = new Mirror(currMirror.x(), z, currMirror.space(), currMirror.transformer());
    }

    public void set(double x, double z, MirrorWriter writer) {
        Mirror currMirror = this.mirror;
        checkStep(writer, "x", currMirror.transformer().coords.x, currMirror.x(), x, currMirror.space());
        checkStep(writer, "z", currMirror.transformer().coords.z, currMirror.z(), z, currMirror.space());
        this.mirror = new Mirror(x, z, currMirror.space(), currMirror.transformer());
    }

    // False until the mirror has been seeded, and false again the moment the player arrives somewhere it was not built
    // for — the one situation where it must be rebased rather than shifted.
    public boolean describes(ResourceKey<Level> dimension) {
        return dimension.equals(this.mirror.space());
    }

    public void rebase(double x, double z, ResourceKey<Level> dimension, WorldLoopTransformer transformer) {
        this.mirror = new Mirror(x, z, dimension, transformer);
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

    private void checkStep(MirrorWriter writer, String axis, WrapDomain domain, double from, double to,
            @Nullable ResourceKey<Level> space) {
        if (domain.fitsInHalf(Math.abs(to - from)) || !warnGate.tryPass()) {
            return;
        }

        Object where = space == null ? "unseeded space" : space.location();
        LOGGER.warn("Half-world step invariant violated in {} by {}: mirror {} stepped from {} to {} without a rebase",
                where, writer.key(), axis, from, to);
    }
}
