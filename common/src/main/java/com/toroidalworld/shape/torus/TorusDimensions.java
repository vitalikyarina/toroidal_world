package com.toroidalworld.shape.torus;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.CarriedShape;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.GenerationOptions;
import com.toroidalworld.core.NetherScales;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.engine.gen.ShapedDimensions;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class TorusDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, TorusSettings settings) {
        GenerationOptions generationOptions = settings.generationOptions();
        WorldDimensions withTorusOverworld = ShapedDimensions.withShape(dimensions, LevelStem.OVERWORLD,
                new CarriedShape(FlatShape.torus(settings.overworld()), generationOptions));
        if (withTorusOverworld == dimensions) {
            return dimensions;
        }

        WorldDimensions withTorusNether = ShapedDimensions.withShape(withTorusOverworld, LevelStem.NETHER,
                new CarriedShape(FlatShape.torus(netherWrapping(settings)), generationOptions));
        return ShapedDimensions.withShape(withTorusNether, LevelStem.END,
                new CarriedShape(FlatShape.torus(settings.end()), generationOptions));
    }

    public static @Nullable TorusSettings read(WorldDimensions dimensions) {
        CarriedShape carried = ShapedDimensions.carriedShapeOf(dimensions, LevelStem.OVERWORLD);
        WorldLoopBounds overworld = carried == null ? null : torusBounds(carried.shape());
        if (overworld == null) {
            return null;
        }

        int overworldChunkWidth = overworld.chunkWidth();
        return new TorusSettings(
                overworld,
                NetherScales.normalize(readNetherScale(dimensions, overworldChunkWidth), overworldChunkWidth),
                readEndWrapping(dimensions),
                carried.generationOptions());
    }

    private static @Nullable WorldLoopBounds torusBoundsOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        return torusBounds(ShapedDimensions.shapeOf(dimensions, key));
    }

    private static @Nullable WorldLoopBounds torusBounds(@Nullable FlatShape shape) {
        if (shape == null || !shape.decomposesPerAxis() || !shape.bounds().isSquare()) {
            return null;
        }

        return shape.bounds();
    }

    private static WorldLoopBounds netherWrapping(TorusSettings settings) {
        int scale = NetherScales.normalize(settings.netherScale(), settings.overworld().chunkWidth());
        return settings.overworld().scaledDown(scale);
    }

    private static WorldLoopBounds readEndWrapping(WorldDimensions dimensions) {
        WorldLoopBounds end = torusBoundsOf(dimensions, LevelStem.END);
        return end != null ? end : TorusSettings.DEFAULT.end();
    }

    private static int readNetherScale(WorldDimensions dimensions, int overworldChunkWidth) {
        WorldLoopBounds nether = torusBoundsOf(dimensions, LevelStem.NETHER);
        return nether != null ? overworldChunkWidth / nether.chunkWidth() : NetherScales.DEFAULT;
    }

    private TorusDimensions() {
    }
}
