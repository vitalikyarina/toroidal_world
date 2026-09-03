package com.toroidalworld.compat.distanthorizons;

import com.toroidalworld.api.ToroidalShape;

import net.minecraft.core.Direction;

public final class DhFold {
    private static final Direction.Axis[] HORIZONTAL = {Direction.Axis.X, Direction.Axis.Z};

    public static int sectionWidthBlocks(byte detailLevel) {
        return 1 << detailLevel;
    }

    public static boolean foldsExactly(ToroidalShape shape, Direction.Axis axis, byte detailLevel) {
        return !shape.loops(axis) || shape.widthBlocks(axis) % sectionWidthBlocks(detailLevel) == 0;
    }

    public static byte maxExactDetailLevel(ToroidalShape shape) {
        int cap = Integer.MAX_VALUE;
        for (Direction.Axis axis : HORIZONTAL) {
            if (shape.loops(axis)) {
                cap = Math.min(cap, Integer.numberOfTrailingZeros(shape.widthBlocks(axis)));
            }
        }

        return (byte) cap;
    }

    public static int foldSection(ToroidalShape shape, Direction.Axis axis, byte detailLevel, int section) {
        if (!shape.loops(axis)) {
            return section;
        }

        int width = sectionWidthBlocks(detailLevel);
        return Math.floorDiv(shape.foldBlock(axis, section * width), width);
    }

    public static int nearestSection(ToroidalShape shape, Direction.Axis axis, byte detailLevel, int refBlock,
            int section) {
        if (!shape.loops(axis)) {
            return section;
        }

        int width = sectionWidthBlocks(detailLevel);
        double nearest = shape.nearestCoord(axis, refBlock, section * width);
        return Math.floorDiv((int) Math.round(nearest), width);
    }

    public static boolean isNearestCopy(ToroidalShape shape, int refBlockX, int refBlockZ, int blockX, int blockZ) {
        return isNearestCoord(shape, Direction.Axis.X, refBlockX, blockX)
                && isNearestCoord(shape, Direction.Axis.Z, refBlockZ, blockZ);
    }

    private static boolean isNearestCoord(ToroidalShape shape, Direction.Axis axis, int ref, int coord) {
        if (!shape.loops(axis)) {
            return true;
        }

        return shape.nearestCoord(axis, ref, coord) == coord && 2L * (coord - ref) != -shape.widthBlocks(axis);
    }

    public static int radiusCapChunks(ToroidalShape shape) {
        int cap = Integer.MAX_VALUE;
        for (Direction.Axis axis : HORIZONTAL) {
            if (shape.loops(axis)) {
                cap = Math.min(cap, shape.widthChunks(axis) / 2);
            }
        }

        return cap;
    }

    private DhFold() {
    }
}
