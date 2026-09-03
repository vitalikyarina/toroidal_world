package com.toroidalworld.options;

import com.toroidalworld.core.CoordinateConstants;

import net.minecraft.world.level.border.WorldBorder;

public final class WorldLoopSizes {
    private static final int MIN_PLAYABLE_VIEW_DISTANCE = 5;

    private static final int MIN_CHUNK_RADIUS = CoordinateConstants.VIEW_DISTANCE_MARGIN + MIN_PLAYABLE_VIEW_DISTANCE;

    private static final int MAX_CHUNK_RADIUS = (int) (WorldBorder.MAX_CENTER_COORDINATE / CoordinateConstants.CHUNK_WIDTH);

    public static final int MIN_CHUNK_WIDTH = MIN_CHUNK_RADIUS * 2;
    public static final int MAX_CHUNK_WIDTH = MAX_CHUNK_RADIUS * 2;

    // Outer islands only generate past 64 chunks (1024 blocks) from the origin, so a narrower End has no progression.
    public static final int END_MIN_CHUNK_WIDTH = 192;
    public static final int END_DEFAULT_CHUNK_WIDTH = 256;

    public static String describe(int chunkWidth) {
        return chunkWidth + " chunks (" + chunkWidth * CoordinateConstants.CHUNK_WIDTH + " blocks)";
    }

    public static boolean isInRange(int chunkWidth) {
        return chunkWidth >= MIN_CHUNK_WIDTH && chunkWidth <= MAX_CHUNK_WIDTH;
    }

    public static boolean isEndInRange(int chunkWidth) {
        return chunkWidth >= END_MIN_CHUNK_WIDTH && chunkWidth <= MAX_CHUNK_WIDTH;
    }

    private WorldLoopSizes() {
    }
}
