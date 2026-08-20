package com.toroidalworld.options;

import com.toroidalworld.core.CoordinateConstants;

public enum WorldLoopPresets {
    TINY("tiny", 32, 2, 256),
    SMALL("small", 64, 4, 320),
    MEDIUM("medium", 128, 8, 384),
    LARGE("large", 256, 8, 448),
    HUGE("huge", 512, 8, 512);

    private final String id;
    private final int chunkWidth;
    private final int netherScale;
    private final int endChunkWidth;

    WorldLoopPresets(String id, int chunkWidth, int netherScale, int endChunkWidth) {
        this.id = id;
        this.chunkWidth = chunkWidth;
        this.netherScale = netherScale;
        this.endChunkWidth = endChunkWidth;
    }

    public String id() {
        return id;
    }

    public int chunkWidth() {
        return chunkWidth;
    }

    public int netherScale() {
        return netherScale;
    }

    public int endChunkWidth() {
        return endChunkWidth;
    }

    public int blockWidth() {
        return chunkWidth * CoordinateConstants.CHUNK_WIDTH;
    }

    public int endBlockWidth() {
        return endChunkWidth() * CoordinateConstants.CHUNK_WIDTH;
    }
}
