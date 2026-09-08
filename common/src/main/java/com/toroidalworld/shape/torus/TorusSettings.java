package com.toroidalworld.shape.torus;

import com.toroidalworld.core.GenerationOptions;
import com.toroidalworld.core.NetherScales;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.WorldLoopSizes;

public record TorusSettings(WorldLoopBounds overworld, int netherScale, WorldLoopBounds end,
        GenerationOptions generationOptions) {
    private static final int DEFAULT_SIZE_CHUNKS = 32;
    private static final GenerationOptions DEFAULT_GENERATION_OPTIONS =
            GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.OFF);

    public static final TorusSettings DEFAULT = new TorusSettings(
            WorldLoopBounds.ofWidth(DEFAULT_SIZE_CHUNKS),
            NetherScales.DEFAULT,
            WorldLoopBounds.ofWidth(WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH),
            DEFAULT_GENERATION_OPTIONS);
}
