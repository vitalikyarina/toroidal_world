package com.toroidalworld.shape.torus;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.api.v1.shape.LoopSpans;
import com.toroidalworld.api.v1.shape.ShapeDimensions;
import com.toroidalworld.core.NetherScales;
import com.toroidalworld.shape.ShapeStems;

import net.minecraft.core.Direction;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class TorusDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, TorusSettings settings) {
        return ShapeDimensions.withSpans(dimensions,
                settings.overworld(),
                ShapeStems.netherSpans(settings.overworld(), settings.netherScale(), settings.chunkWidth()),
                settings.end(),
                settings.generationOptions());
    }

    public static @Nullable TorusSettings read(WorldDimensions dimensions) {
        LoopSpans overworld = ShapeStems.spansOf(dimensions, LevelStem.OVERWORLD, LoopSpans::isSquare);
        if (overworld == null) {
            return null;
        }

        int overworldChunkWidth = overworld.chunkWidth(Direction.Axis.X);
        int netherScale = ShapeStems.readNetherScale(dimensions, LoopSpans::isSquare, Direction.Axis.X,
                overworldChunkWidth);
        return new TorusSettings(
                overworld,
                NetherScales.normalize(netherScale, overworldChunkWidth),
                readEndSpans(dimensions),
                ShapeDimensions.optionsOf(dimensions, LevelStem.OVERWORLD));
    }

    private static LoopSpans readEndSpans(WorldDimensions dimensions) {
        LoopSpans end = ShapeStems.spansOf(dimensions, LevelStem.END, LoopSpans::isSquare);
        return end != null ? end : TorusSettings.DEFAULT.end();
    }

    private TorusDimensions() {
    }
}
