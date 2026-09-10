package com.toroidalworld.shape.torus;

import com.toroidalworld.api.v1.option.GenerationOptions;
import com.toroidalworld.api.v1.shape.LoopSpans;
import com.toroidalworld.core.NetherScales;
import com.toroidalworld.core.WorldLoopSizes;

import net.minecraft.core.Direction;

public record TorusSettings(LoopSpans overworld, int netherScale, LoopSpans end,
        GenerationOptions generationOptions) {
    private static final GenerationOptions DEFAULT_GENERATION_OPTIONS =
            GenerationOptions.DEFAULT.with(CompactBiomes.OPTION, ClimateScale.OFF);

    public static final TorusSettings DEFAULT = new TorusSettings(
            LoopSpans.ofWidth(WorldLoopSizes.DEFAULT_CHUNK_WIDTH),
            NetherScales.DEFAULT,
            LoopSpans.ofWidth(WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH),
            DEFAULT_GENERATION_OPTIONS);

    public int chunkWidth() {
        return overworld.chunkWidth(Direction.Axis.X);
    }

    public int endChunkWidth() {
        return end.chunkWidth(Direction.Axis.X);
    }
}
