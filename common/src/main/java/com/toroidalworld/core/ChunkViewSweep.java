package com.toroidalworld.core;

import net.minecraft.world.level.ChunkPos;

public sealed interface ChunkViewSweep {
    int NEIGHBOUR_RING = 1;

    record OverBoth(int minX, int minZ, int maxX, int maxZ) implements ChunkViewSweep {
        @Override
        public long positions() {
            return ((long) maxX - minX + 1) * ((long) maxZ - minZ + 1);
        }
    }

    record EachAlone(int previousViewDistance, int nextViewDistance) implements ChunkViewSweep {
        @Override
        public long positions() {
            long previousSpan = span(previousViewDistance);
            long nextSpan = span(nextViewDistance);
            return previousSpan * previousSpan + nextSpan * nextSpan;
        }
    }

    long positions();

    static ChunkViewSweep between(WorldFold fold, ChunkPos previousCenter, int previousViewDistance,
            ChunkPos nextCenter, int nextViewDistance) {
        ChunkPos nearestNextCenter = fold.nearestCopy(previousCenter, nextCenter);
        int previousRadius = previousViewDistance + NEIGHBOUR_RING;
        int nextRadius = nextViewDistance + NEIGHBOUR_RING;

        long reach = (long) previousRadius + nextRadius;
        if (Math.abs((long) nearestNextCenter.x - previousCenter.x) > reach
                || Math.abs((long) nearestNextCenter.z - previousCenter.z) > reach) {
            return new EachAlone(previousViewDistance, nextViewDistance);
        }

        return new OverBoth(
                Math.min(previousCenter.x - previousRadius, nearestNextCenter.x - nextRadius),
                Math.min(previousCenter.z - previousRadius, nearestNextCenter.z - nextRadius),
                Math.max(previousCenter.x + previousRadius, nearestNextCenter.x + nextRadius),
                Math.max(previousCenter.z + previousRadius, nearestNextCenter.z + nextRadius));
    }

    private static int span(int viewDistance) {
        return 2 * (viewDistance + NEIGHBOUR_RING) + 1;
    }
}
