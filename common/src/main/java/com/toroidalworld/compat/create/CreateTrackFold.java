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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CreateTrackFold {
    // TrackNodeLocation stores Math.round(coord * 2) — the rail ends of one block are one unit apart on each side of
    // its centre, so every node coordinate is a whole number of half-blocks.
    private static final int NODE_KEY_UNITS_PER_BLOCK = 2;

    private static volatile @Nullable NodeKeyMemo nodeKeyMemo;

    public record NodeKeyAxes(WrapDomain x, WrapDomain z) {
    }

    private record NodeKeyMemo(WorldLoopTransformer transformer, NodeKeyAxes axes) {
    }

    public static @Nullable WorldLoopTransformer transformerOf(@Nullable Level level,
            @Nullable ResourceKey<Level> dimension) {
        if (level != null) {
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
        NodeKeyMemo memo = nodeKeyMemo;
        if (memo != null && memo.transformer() == transformer) {
            return memo.axes();
        }

        NodeKeyAxes axes =
                new NodeKeyAxes(nodeKeyDomain(transformer.bounds.x()), nodeKeyDomain(transformer.bounds.z()));
        nodeKeyMemo = new NodeKeyMemo(transformer, axes);
        return axes;
    }

    public static Vec3 nearestCopy(@Nullable Level level, Vec3 anchor, Vec3 target) {
        return nearestCopy(transformerOf(level, null), anchor, target);
    }

    public static Vec3 nearestCopy(@Nullable ResourceKey<Level> dimension, Vec3 anchor, Vec3 target) {
        return nearestCopy(transformerOf(null, dimension), anchor, target);
    }

    public static BlockPos nearestCopy(@Nullable Level level, BlockPos anchor, BlockPos target) {
        WorldLoopTransformer transformer = transformerOf(level, null);
        return transformer == null ? target : transformer.blocks.nearestCopy(anchor, target);
    }

    private static Vec3 nearestCopy(@Nullable WorldLoopTransformer transformer, Vec3 anchor, Vec3 target) {
        return transformer == null ? target : transformer.vectors.nearestCopy(anchor, target);
    }

    public static AABB foldBoxToward(@Nullable Level level, Vec3 anchor, AABB box) {
        WorldLoopTransformer transformer = transformerOf(level, null);
        return transformer == null ? box : transformer.foldBoxToward(anchor, box);
    }

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
