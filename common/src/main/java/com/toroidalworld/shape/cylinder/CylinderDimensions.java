package com.toroidalworld.shape.cylinder;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.CarriedShape;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.NetherScales;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.WorldLoopSizes;
import com.toroidalworld.engine.gen.ShapedDimensions;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class CylinderDimensions {

    public static WorldDimensions apply(WorldDimensions dimensions, CylinderSettings settings) {
        return ShapedDimensions.withShapes(dimensions,
                new CarriedShape(FlatShape.cylinder(settings.overworld())),
                new CarriedShape(FlatShape.cylinder(netherWrapping(settings))),
                new CarriedShape(FlatShape.cylinder(settings.end())));
    }

    public static @Nullable CylinderSettings read(WorldDimensions dimensions) {
        WorldLoopBounds overworld = cylinderBoundsOf(dimensions, LevelStem.OVERWORLD);
        if (overworld == null) {
            return null;
        }

        Direction.Axis axis = CylinderSettings.loopedAxis(overworld);
        int overworldChunkWidth = overworld.chunkWidth(axis);
        return new CylinderSettings(
                overworld,
                NetherScales.normalize(readNetherScale(dimensions, axis, overworldChunkWidth), overworldChunkWidth),
                readEndWrapping(dimensions, axis));
    }

    private static @Nullable WorldLoopBounds cylinderBoundsOf(WorldDimensions dimensions, ResourceKey<LevelStem> key) {
        FlatShape shape = ShapedDimensions.shapeOf(dimensions, key);
        return shape != null && CylinderSettings.isCylinder(shape) ? shape.bounds() : null;
    }

    private static WorldLoopBounds netherWrapping(CylinderSettings settings) {
        int scale = NetherScales.normalize(settings.netherScale(), settings.chunkWidth());
        return settings.overworld().scaledDown(scale);
    }

    private static WorldLoopBounds readEndWrapping(WorldDimensions dimensions, Direction.Axis axis) {
        WorldLoopBounds end = cylinderBoundsOf(dimensions, LevelStem.END);
        return end != null && end.loops(axis)
                ? end
                : WorldLoopBounds.ofWidth(axis, WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH);
    }

    private static int readNetherScale(WorldDimensions dimensions, Direction.Axis axis, int overworldChunkWidth) {
        WorldLoopBounds nether = cylinderBoundsOf(dimensions, LevelStem.NETHER);
        return nether != null && nether.loops(axis)
                ? overworldChunkWidth / nether.chunkWidth(axis)
                : NetherScales.DEFAULT;
    }

    private CylinderDimensions() {
    }
}
