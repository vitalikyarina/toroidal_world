package com.toroidalworld.compat.create;

import com.toroidalworld.core.WorldFold;

import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;

public final class CreateTrackFold {
    // TrackNodeLocation stores Math.round(coord * 2) — the rail ends of one block are one unit apart on each side of
    // its centre, so every node coordinate is a whole number of half-blocks.
    private static final int NODE_KEY_UNITS_PER_BLOCK = 2;

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
