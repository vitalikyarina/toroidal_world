package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.storage.CurrentServer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

// What Create's track graph needs from us, in two statements that answer two different questions about one node.
//
// A node location is both the key the graph files a rail end under and the geometry every length, position and
// direction is measured from, and one fold cannot serve both. As a key it must be canonical — the rail end at the seam
// is one physical place, and the graph is a HashMap that has to file it once however the walk arrived at it. As
// geometry it must stay continuous — the two ends of a rail that crosses the seam are one block apart, and canonical
// keys name them 511 blocks apart. So the key is folded into the world's bounds when it is built, and each pair of
// locations is put back into one frame at the moment they are combined.
//
// The key counts half-blocks: TrackNodeLocation rounds a block coordinate to the nearer half before storing it, so a
// 512-block world is 1024 units wide here and the transformer's block domains are wrong by a factor of two. WrapDomain
// is the per-axis primitive underneath those, and the axis bounds are chunk spans, so the domain this unit needs is
// built here rather than borrowed. It is built per call: a node key is folded when a rail is placed or a graph is
// loaded, never per tick, and the geometry fold below — which is per tick — works in blocks and uses the transformer's
// own vector domain.
//
// The dimension is the only handle a node location carries, and it is set after the constructor has run, so the fold
// is taken where the dimension arrives rather than where the coordinates do. A caller holding the level hands it over;
// one holding only the key reaches the level through the running server, the way a reloaded map does.
public final class CreateTrackFold {
    // TrackNodeLocation stores Math.round(coord * 2) — the rail ends of one block are one unit apart on each side of
    // its centre, so every node coordinate is a whole number of half-blocks.
    private static final int NODE_KEY_UNITS_PER_BLOCK = 2;

    // The two horizontal axes of the key's own unit, kept together because a fold is only ever taken on both at once.
    public record NodeKeyAxes(WrapDomain x, WrapDomain z) {
    }

    public static @Nullable WorldLoopTransformer transformerOf(@Nullable Level level,
            @Nullable ResourceKey<Level> dimension) {
        if (level != null) {
            // The client's own transformer is NOOP by design, so a client level answers by the bounds the server sent
            // it; a server level has no client bounds and falls through to its own.
            WorldLoopTransformer clientBounds = WorldLoopAttachments.wrappedClientBoundsTransformerOf(level);
            return clientBounds != null ? clientBounds : WorldLoopAttachments.wrappedTransformerOf(level);
        }

        if (dimension == null) {
            return null;
        }

        MinecraftServer server = CurrentServer.get();
        if (server == null) {
            return null;
        }

        ServerLevel serverLevel = server.getLevel(dimension);
        return serverLevel == null ? null : WorldLoopAttachments.wrappedTransformerOf(serverLevel);
    }

    public static NodeKeyAxes nodeKeyAxes(WorldLoopTransformer transformer) {
        return new NodeKeyAxes(nodeKeyDomain(transformer.bounds.x()), nodeKeyDomain(transformer.bounds.z()));
    }

    // The copy of a node's position nearest the one it is being measured against — the edge's other end, the block a
    // signal sits on, the node the walk stepped from. Handed the target itself back when the pair is already the short
    // way apart, which is every pair that does not straddle the seam.
    public static Vec3 nearestCopy(@Nullable Level level, Vec3 anchor, Vec3 target) {
        return nearestCopy(transformerOf(level, null), anchor, target);
    }

    public static Vec3 nearestCopy(@Nullable ResourceKey<Level> dimension, Vec3 anchor, Vec3 target) {
        return nearestCopy(transformerOf(null, dimension), anchor, target);
    }

    // The same question asked of a block, for the one caller that has to fold a position before it has any geometry —
    // placement, which measures the gap between two clicked blocks before it will draw anything between them.
    public static BlockPos nearestCopy(@Nullable Level level, BlockPos anchor, BlockPos target) {
        WorldLoopTransformer transformer = transformerOf(level, null);
        return transformer == null ? target : transformer.blocks.nearestCopy(anchor, target);
    }

    private static Vec3 nearestCopy(@Nullable WorldLoopTransformer transformer, Vec3 anchor, Vec3 target) {
        return transformer == null ? target : transformer.vectors.nearestCopy(anchor, target);
    }

    // The world's own frame, for the one step where a position stops being graph state and becomes an entity's place in
    // the world. The graph keeps its edges continuous and lets a position run a little past the bounds, which is what
    // makes the arithmetic along an edge ordinary; the entity may not, because everything downstream of it — the chunk
    // it ticks in, the tracker, the save — is keyed by ground the world actually has.
    //
    // The level's own transformer, and deliberately not the bounds the client was told: this is the one question in
    // this class that is about where a thing may exist rather than about which copy of it is meant. A client level's
    // transformer is NOOP by design, because the client is told the world is infinite and renders straight through the
    // seam — so on that side there is nothing to wrap, and wrapping anyway would drag the carriage a world away from
    // the player following it across.
    public static Vec3 wrap(@Nullable Level level, Vec3 position) {
        WorldLoopTransformer transformer = level == null ? null : WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? position : transformer.vectors.wrap(position);
    }

    private static WrapDomain nodeKeyDomain(AxisBounds axis) {
        return switch (axis) {
            case AxisBounds.Looped looped -> new WrapDomain(
                    looped.minChunk() * CoordinateConstants.CHUNK_WIDTH * NODE_KEY_UNITS_PER_BLOCK,
                    looped.maxChunk() * CoordinateConstants.CHUNK_WIDTH * NODE_KEY_UNITS_PER_BLOCK);
            case AxisBounds.Unbounded() -> new WrapDomain.Noop();
        };
    }

    private CreateTrackFold() {
    }
}
