package com.toroidalworld.shape.torus;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.api.v1.shape.LoopSpans;
import com.toroidalworld.api.v1.shape.ShapeDimensions;
import com.toroidalworld.core.NetherScales;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class TorusDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, TorusSettings settings) {
        return ShapeDimensions.withSpans(dimensions,
                settings.overworld(),
                netherSpans(settings),
                settings.end(),
                settings.generationOptions());
    }

    public static @Nullable TorusSettings read(WorldDimensions dimensions) {
        LoopSpans overworld = torusSpansOf(dimensions, LevelStem.OVERWORLD);
        if (overworld == null) {
            return null;
        }

        int overworldChunkWidth = overworld.chunkWidth(Direction.Axis.X);
        return new TorusSettings(
                overworld,
                NetherScales.normalize(readNetherScale(dimensions, overworldChunkWidth), overworldChunkWidth),
                readEndSpans(dimensions),
                ShapeDimensions.optionsOf(dimensions, LevelStem.OVERWORLD));
    }

    private static @Nullable LoopSpans torusSpansOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        LoopSpans spans = ShapeDimensions.spansOf(dimensions, key);
        return spans != null && spans.isSquare() ? spans : null;
    }

    private static LoopSpans netherSpans(TorusSettings settings) {
        int scale = NetherScales.normalize(settings.netherScale(), settings.chunkWidth());
        return settings.overworld().scaledDown(scale);
    }

    private static LoopSpans readEndSpans(WorldDimensions dimensions) {
        LoopSpans end = torusSpansOf(dimensions, LevelStem.END);
        return end != null ? end : TorusSettings.DEFAULT.end();
    }

    private static int readNetherScale(WorldDimensions dimensions, int overworldChunkWidth) {
        LoopSpans nether = torusSpansOf(dimensions, LevelStem.NETHER);
        return nether != null
                ? overworldChunkWidth / nether.chunkWidth(Direction.Axis.X)
                : NetherScales.DEFAULT;
    }

    private TorusDimensions() {
    }
}
