package com.toroidalworld.compat;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.core.CoordinateConstants;

import net.minecraft.core.Direction;

public final class FullscreenZoomFloor {
    public static final int MIN_WORLD_PIXELS = 64;

    public static final int JOURNEYMAP_REGION_BLOCKS = 512;

    public static int journeyMapZoom(ToroidalShape shape) {
        int floor = 0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (shape.loops(axis)) {
                floor = Math.max(floor, journeyMapZoom(shape.widthBlocks(axis)));
            }
        }

        return floor;
    }

    public static double xaeroScale(ToroidalShape shape, double scaleMultiplier) {
        double floor = 0.0;
        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
            if (shape.loops(axis)) {
                floor = Math.max(floor, xaeroScale(shape.widthBlocks(axis), scaleMultiplier));
            }
        }

        return floor;
    }

    static int journeyMapZoom(int widthBlocks) {
        return Math.ceilDiv(MIN_WORLD_PIXELS * JOURNEYMAP_REGION_BLOCKS, widthBlocks);
    }

    static double xaeroScale(int widthBlocks, double scaleMultiplier) {
        return MIN_WORLD_PIXELS / (widthBlocks * scaleMultiplier);
    }

    private FullscreenZoomFloor() {
    }
}
