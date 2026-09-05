package com.toroidalworld.compat.distanthorizons;

import com.toroidalworld.api.ToroidalShape;

import net.minecraft.core.Direction;

public final class DhFold {
    private static final Direction.Axis[] HORIZONTAL = {Direction.Axis.X, Direction.Axis.Z};

    public static int sectionWidthBlocks(byte detailLevel) {
        return 1 << detailLevel;
    }

    public static byte maxExactDetailLevel(ToroidalShape shape) {
        int cap = Byte.MAX_VALUE;
        for (Direction.Axis axis : HORIZONTAL) {
            if (shape.loops(axis)) {
                cap = Math.min(cap, Integer.numberOfTrailingZeros(shape.widthBlocks(axis)));
                cap = Math.min(cap, Integer.numberOfTrailingZeros(shape.minBlock(axis)));
            }
        }

        return (byte) cap;
    }

    public static byte maxRenderableDetailLevel(ToroidalShape shape, byte leafDetailLevel) {
        return (byte) Math.max(maxExactDetailLevel(shape), leafDetailLevel);
    }

    public static byte maxExpectedDetailLevel(ToroidalShape shape, byte leafDetailLevel) {
        return (byte) (maxRenderableDetailLevel(shape, leafDetailLevel) - leafDetailLevel);
    }

    public static boolean keysFoldWithoutCollision(ToroidalShape shape, byte leafDetailLevel) {
        return maxExactDetailLevel(shape) >= leafDetailLevel;
    }

    public static boolean foldKeepsTheGrid(ToroidalShape shape, Direction.Axis axis, byte detailLevel, int section) {
        if (!shape.loops(axis)) {
            return true;
        }

        int width = sectionWidthBlocks(detailLevel);
        return Math.floorMod(shape.foldBlock(axis, section * width), width) == 0;
    }

    public static boolean foldKeepsTheSpan(ToroidalShape shape, Direction.Axis axis, byte detailLevel, int section) {
        if (!shape.loops(axis)) {
            return true;
        }

        int width = sectionWidthBlocks(detailLevel);
        int last = shape.foldBlock(axis, section * width) + width - 1;
        return shape.foldBlock(axis, last) == last;
    }

    public static boolean isAddressableSection(ToroidalShape shape, byte detailLevel, int sectionX, int sectionZ) {
        return foldKeepsTheGrid(shape, Direction.Axis.X, detailLevel, sectionX)
                && foldKeepsTheSpan(shape, Direction.Axis.X, detailLevel, sectionX)
                && foldKeepsTheGrid(shape, Direction.Axis.Z, detailLevel, sectionZ)
                && foldKeepsTheSpan(shape, Direction.Axis.Z, detailLevel, sectionZ);
    }

    public static int foldSection(ToroidalShape shape, Direction.Axis axis, byte detailLevel, int section) {
        if (!shape.loops(axis) || detailLevel > maxExactDetailLevel(shape)) {
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

    public static boolean overlapsNearestLap(ToroidalShape shape, Direction.Axis axis, int refBlock, int minBlock,
            int widthBlocks) {
        if (!shape.loops(axis)) {
            return true;
        }

        long half = shape.widthBlocks(axis) / 2L;
        return minBlock < refBlock + half && minBlock + (long) widthBlocks > refBlock - half;
    }

    private DhFold() {
    }
}
