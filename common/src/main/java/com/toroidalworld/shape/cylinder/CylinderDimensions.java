package com.toroidalworld.shape.cylinder;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.api.v1.option.GenerationOptions;
import com.toroidalworld.api.v1.shape.LoopSpans;
import com.toroidalworld.api.v1.shape.ShapeDimensions;
import com.toroidalworld.core.NetherScales;
import com.toroidalworld.core.WorldLoopSizes;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class CylinderDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, CylinderSettings settings) {
        return ShapeDimensions.withSpans(dimensions,
                settings.overworld(),
                netherSpans(settings),
                settings.end(),
                GenerationOptions.DEFAULT);
    }

    public static @Nullable CylinderSettings read(WorldDimensions dimensions) {
        LoopSpans overworld = cylinderSpansOf(dimensions, LevelStem.OVERWORLD);
        if (overworld == null) {
            return null;
        }

        Direction.Axis axis = CylinderSettings.loopedAxis(overworld);
        int overworldChunkWidth = overworld.chunkWidth(axis);
        return new CylinderSettings(
                overworld,
                NetherScales.normalize(readNetherScale(dimensions, axis, overworldChunkWidth), overworldChunkWidth),
                readEndSpans(dimensions, axis));
    }

    private static @Nullable LoopSpans cylinderSpansOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        LoopSpans spans = ShapeDimensions.spansOf(dimensions, key);
        return spans != null && CylinderSettings.isCylinder(spans) ? spans : null;
    }

    private static LoopSpans netherSpans(CylinderSettings settings) {
        int scale = NetherScales.normalize(settings.netherScale(), settings.chunkWidth());
        return settings.overworld().scaledDown(scale);
    }

    private static LoopSpans readEndSpans(WorldDimensions dimensions, Direction.Axis axis) {
        LoopSpans end = cylinderSpansOf(dimensions, LevelStem.END);
        return end != null && end.loops(axis)
                ? end
                : LoopSpans.ofWidth(axis, WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH);
    }

    private static int readNetherScale(WorldDimensions dimensions, Direction.Axis axis, int overworldChunkWidth) {
        LoopSpans nether = cylinderSpansOf(dimensions, LevelStem.NETHER);
        return nether != null && nether.loops(axis)
                ? overworldChunkWidth / nether.chunkWidth(axis)
                : NetherScales.DEFAULT;
    }

    private CylinderDimensions() {
    }
}
