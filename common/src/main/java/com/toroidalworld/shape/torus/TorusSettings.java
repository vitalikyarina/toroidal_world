package com.toroidalworld.shape.torus;

import com.toroidalworld.options.NetherScales;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopSizes;

public record TorusSettings(WorldLoopBounds overworld, int netherScale, WorldLoopBounds end) {
    private static final int DEFAULT_SIZE_CHUNKS = 32;

    public static final TorusSettings DEFAULT = new TorusSettings(
            WorldLoopBounds.ofWidth(DEFAULT_SIZE_CHUNKS),
            NetherScales.DEFAULT,
            WorldLoopBounds.ofWidth(WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH));
}
