package com.toroidalworld.compat.create;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.CurrentServer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
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

    public static @Nullable WorldFold transformerOf(@Nullable Level level,
            @Nullable ResourceKey<Level> dimension) {
        if (level != null) {
            return WorldLoopAttachments.wrappedTransformerOfReader(level);
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

    public static Vec3i canonicalNodeKey(WorldFold transformer, Vec3i key) {
        Vec3 location = nodeKeyLocation(key);
        if (!transformer.isOver(location)) {
            return key;
        }

        return nodeKeyAt(transformer.fold(location), key.getY());
    }

    public static Vec3i nearestNodeKey(WorldFold transformer, Vec3i anchor, Vec3i key) {
        Vec3 location = nodeKeyLocation(key);
        Vec3 nearest = transformer.nearestCopy(nodeKeyLocation(anchor), location);
        return nearest == location ? key : nodeKeyAt(nearest, key.getY());
    }

    public static Vec3 nearestCopy(@Nullable Level level, Vec3 anchor, Vec3 target) {
        return nearestCopy(transformerOf(level, null), anchor, target);
    }

    public static Vec3 nearestCopy(@Nullable ResourceKey<Level> dimension, Vec3 anchor, Vec3 target) {
        return nearestCopy(transformerOf(null, dimension), anchor, target);
    }

    public static Vec3 nearestCopy(@Nullable Level level, @Nullable ResourceKey<Level> dimension, Vec3 anchor,
            Vec3 target) {
        return nearestCopy(transformerOf(level, dimension), anchor, target);
    }

    public static BlockPos nearestCopy(@Nullable Level level, BlockPos anchor, BlockPos target) {
        WorldFold transformer = transformerOf(level, null);
        return transformer == null ? target : transformer.nearestCopy(anchor, target);
    }

    private static Vec3 nearestCopy(@Nullable WorldFold transformer, Vec3 anchor, Vec3 target) {
        return transformer == null ? target : transformer.nearestCopy(anchor, target);
    }

    public static AABB foldBoxToward(@Nullable Level level, Vec3 anchor, AABB box) {
        WorldFold transformer = transformerOf(level, null);
        return transformer == null ? box : transformer.foldBox(anchor, box).value();
    }

    public static Vec3 wrap(@Nullable ServerLevel level, Vec3 position) {
        WorldFold transformer = level == null ? null : WorldLoopAttachments.wrappedTransformerOf(level);
        return transformer == null ? position : transformer.fold(position);
    }

    private static Vec3 nodeKeyLocation(Vec3i key) {
        return new Vec3((double) key.getX() / NODE_KEY_UNITS_PER_BLOCK, 0.0,
                (double) key.getZ() / NODE_KEY_UNITS_PER_BLOCK);
    }

    private static Vec3i nodeKeyAt(Vec3 location, int keyY) {
        return new Vec3i(nodeKeyUnits(location.x), keyY, nodeKeyUnits(location.z));
    }

    private static int nodeKeyUnits(double coord) {
        return (int) Math.round(coord * NODE_KEY_UNITS_PER_BLOCK);
    }

    private CreateTrackFold() {
    }
}
