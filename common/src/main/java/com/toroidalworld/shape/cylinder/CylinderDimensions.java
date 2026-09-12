package com.toroidalworld.shape.cylinder;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.api.v1.option.GenerationOptions;
import com.toroidalworld.api.v1.shape.LoopSpans;
import com.toroidalworld.api.v1.shape.ShapeDimensions;
import com.toroidalworld.core.NetherScales;
import com.toroidalworld.core.WorldLoopSizes;
import com.toroidalworld.shape.ShapeStems;

import net.minecraft.core.Direction;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class CylinderDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, CylinderSettings settings) {
        return ShapeDimensions.withSpans(dimensions,
                settings.overworld(),
                ShapeStems.netherSpans(settings.overworld(), settings.netherScale(), settings.chunkWidth()),
                settings.end(),
                GenerationOptions.DEFAULT);
    }

    public static @Nullable CylinderSettings read(WorldDimensions dimensions) {
        LoopSpans overworld = ShapeStems.spansOf(dimensions, LevelStem.OVERWORLD, CylinderSettings::isCylinder);
        if (overworld == null) {
            return null;
        }

        Direction.Axis axis = CylinderSettings.loopedAxis(overworld);
        int overworldChunkWidth = overworld.chunkWidth(axis);
        int netherScale = ShapeStems.readNetherScale(dimensions, CylinderSettings::isCylinder, axis,
                overworldChunkWidth);
        return new CylinderSettings(
                overworld,
                NetherScales.normalize(netherScale, overworldChunkWidth),
                readEndSpans(dimensions, axis));
    }

    private static LoopSpans readEndSpans(WorldDimensions dimensions, Direction.Axis axis) {
        LoopSpans end = ShapeStems.spansOf(dimensions, LevelStem.END, CylinderSettings::isCylinder);
        return end != null && end.loops(axis)
                ? end
                : LoopSpans.ofWidth(axis, WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH);
    }

    private CylinderDimensions() {
    }
}
