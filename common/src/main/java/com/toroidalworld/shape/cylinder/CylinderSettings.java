package com.toroidalworld.shape.cylinder;

import com.toroidalworld.api.v1.shape.LoopSpans;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.NetherScales;
import com.toroidalworld.core.WorldLoopSizes;

import net.minecraft.core.Direction;

public record CylinderSettings(LoopSpans overworld, int netherScale, LoopSpans end) {
    private static final Direction.Axis DEFAULT_AXIS = Direction.Axis.X;

    public static final CylinderSettings DEFAULT = new CylinderSettings(
            LoopSpans.ofWidth(DEFAULT_AXIS, WorldLoopSizes.DEFAULT_CHUNK_WIDTH),
            NetherScales.DEFAULT,
            LoopSpans.ofWidth(DEFAULT_AXIS, WorldLoopSizes.END_DEFAULT_CHUNK_WIDTH));

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

    public static boolean isCylinder(LoopSpans spans) {
        return spans.loops(Direction.Axis.X) != spans.loops(Direction.Axis.Z);
    }

    public static boolean isCylinder(FlatShape shape) {
        return shape.identification() == FlatShape.Identification.CYLINDER;
    }

    public static Direction.Axis loopedAxis(LoopSpans spans) {
        return spans.loops(Direction.Axis.X) ? Direction.Axis.X : Direction.Axis.Z;
    }
}
