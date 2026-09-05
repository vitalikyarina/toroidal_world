package com.toroidalworld.shape.torus;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.gen.ShapedDimensions;
import com.toroidalworld.options.NetherScales;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class TorusDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, TorusSettings settings) {
        WorldDimensions withTorusOverworld =
                ShapedDimensions.withShape(dimensions, LevelStem.OVERWORLD, FlatShape.torus(settings.overworld()));
        if (withTorusOverworld == dimensions) {
            return dimensions;
        }

        WorldDimensions withTorusNether =
                ShapedDimensions.withShape(withTorusOverworld, LevelStem.NETHER, FlatShape.torus(netherWrapping(settings)));
        return ShapedDimensions.withShape(withTorusNether, LevelStem.END, FlatShape.torus(settings.end()));
    }

    public static @Nullable TorusSettings read(WorldDimensions dimensions) {
        WorldLoopBounds overworld = torusBoundsOf(dimensions, LevelStem.OVERWORLD);
        if (overworld == null) {
            return null;
        }

        int overworldChunkWidth = overworld.chunkWidth();
        return new TorusSettings(
                overworld,
                NetherScales.normalize(readNetherScale(dimensions, overworldChunkWidth), overworldChunkWidth),
                readEndWrapping(dimensions));
    }

    private static @Nullable WorldLoopBounds torusBoundsOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        FlatShape shape = ShapedDimensions.shapeOf(dimensions, key);
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
