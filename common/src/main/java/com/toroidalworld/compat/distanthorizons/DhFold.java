package com.toroidalworld.compat.distanthorizons;

import com.toroidalworld.api.ToroidalShape;

import net.minecraft.core.Direction;

public final class DhFold {
    private static final Direction.Axis[] HORIZONTAL = {Direction.Axis.X, Direction.Axis.Z};
    private static final int SNAP_CELLS_PER_WORLD = 16;

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

    public static byte snapDetailLevel(ToroidalShape shape, byte leafDetailLevel) {
        int narrowest = Integer.MAX_VALUE;
        for (Direction.Axis axis : HORIZONTAL) {
            if (shape.loops(axis)) {
                narrowest = Math.min(narrowest, shape.widthBlocks(axis));
            }
        }

        if (narrowest == Integer.MAX_VALUE) {
            return leafDetailLevel;
        }

        int cell = narrowest / SNAP_CELLS_PER_WORLD;
        int level = cell <= 0 ? 0 : Integer.SIZE - 1 - Integer.numberOfLeadingZeros(cell);
        return (byte) Math.max(level, leafDetailLevel);
    }

    public static int nearestSection(ToroidalShape shape, Direction.Axis axis, byte snapLevel, byte detailLevel,
            int refBlock, int section) {
        if (!shape.loops(axis) || detailLevel > maxExactDetailLevel(shape)) {
            return section;
        }

        int worldWidth = shape.widthBlocks(axis);
        long laps = detailLevel <= snapLevel
                ? lapsToward(worldWidth, snapCellCentreBlock(snapLevel, detailLevel, section) - refBlock)
                : lapsToward(worldWidth, sectionCentreBlock(detailLevel, section) - refBlock);
        return (int) (section - laps * (worldWidth / sectionWidthBlocks(detailLevel)));
    }

    public static boolean isNearestSection(ToroidalShape shape, Direction.Axis axis, byte snapLevel,
            byte detailLevel, int refBlock, int section) {
        if (!shape.loops(axis)) {
            return true;
        }

        long worldWidth = shape.widthBlocks(axis);
        if (detailLevel <= snapLevel) {
            return lapsToward(worldWidth, snapCellCentreBlock(snapLevel, detailLevel, section) - refBlock) == 0;
        }

        long first = sectionCentreBlock(snapLevel, firstCell(snapLevel, detailLevel, section));
        long last = sectionCentreBlock(snapLevel, lastCell(snapLevel, detailLevel, section));
        return lapsToward(worldWidth, first - refBlock) == 0 && lapsToward(worldWidth, last - refBlock) == 0;
    }

    public static boolean overlapsNearestWindow(ToroidalShape shape, Direction.Axis axis, byte snapLevel,
            byte detailLevel, int refBlock, int section) {
        if (!shape.loops(axis)) {
            return true;
        }

        if (detailLevel <= snapLevel) {
            return isNearestSection(shape, axis, snapLevel, detailLevel, refBlock, section);
        }

        long worldWidth = shape.widthBlocks(axis);
        long first = sectionCentreBlock(snapLevel, firstCell(snapLevel, detailLevel, section));
        long last = sectionCentreBlock(snapLevel, lastCell(snapLevel, detailLevel, section));
        return lapsToward(worldWidth, first - refBlock) <= 0 && lapsToward(worldWidth, last - refBlock) >= 0;
    }

    private static long snapCellCentreBlock(byte snapLevel, byte detailLevel, int section) {
        return sectionCentreBlock(snapLevel, section >> (snapLevel - detailLevel));
    }

    private static long firstCell(byte snapLevel, byte detailLevel, int section) {
        return (long) section << (detailLevel - snapLevel);
    }

    private static long lastCell(byte snapLevel, byte detailLevel, int section) {
        return firstCell(snapLevel, detailLevel, section) + (1L << (detailLevel - snapLevel)) - 1;
    }

    private static long sectionCentreBlock(byte detailLevel, long section) {
        int width = sectionWidthBlocks(detailLevel);
        return section * width + width / 2;
    }

    private static long lapsToward(long worldWidth, long delta) {
        return Math.floorDiv(2 * delta - 1 + worldWidth, 2 * worldWidth);
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
