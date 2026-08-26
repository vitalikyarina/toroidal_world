package com.toroidalworld.shape.cylinder;

import com.toroidalworld.options.NetherScales;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopSizes;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Direction;

public record CylinderSettings(WorldLoopBounds overworld, int netherScale, WorldLoopBounds end) {
    private static final Direction.Axis DEFAULT_AXIS = Direction.Axis.X;
    private static final int DEFAULT_SIZE_CHUNKS = 32;

    public static final CylinderSettings DEFAULT = new CylinderSettings(
            WorldLoopBounds.ofWidth(DEFAULT_AXIS, DEFAULT_SIZE_CHUNKS),
            NetherScales.DEFAULT,
            WorldLoopBounds.ofWidth(DEFAULT_AXIS, WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH));

    public CylinderSettings {
        if (!isCylinder(overworld)) {
            throw new IllegalArgumentException("A cylinder loops on exactly one axis, got " + overworld);
        }

        if (!isCylinder(end) || !end.loops(loopedAxis(overworld))) {
            throw new IllegalArgumentException("The End loops on the overworld axis " + loopedAxis(overworld)
                    + ", got " + end);
        }
    }

    public Direction.Axis axis() {
        return loopedAxis(overworld);
    }

    public int chunkWidth() {
        return overworld.chunkWidth(axis());
    }

    public int endChunkWidth() {
        return end.chunkWidth(axis());
    }

    public static boolean isCylinder(WorldLoopBounds bounds) {
        return bounds.loops(Direction.Axis.X) != bounds.loops(Direction.Axis.Z);
    }

    public static boolean isCylinder(FlatShape shape) {
        return shape.identification() == FlatShape.Identification.CYLINDER;
    }

    public static Direction.Axis loopedAxis(WorldLoopBounds bounds) {
        return bounds.loops(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
    }
}
