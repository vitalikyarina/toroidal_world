package com.toroidalworld.compat;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.engine.seam.MapSurfaceCopies.Copies;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public final class MapCopyBudget {
    private static final int MAX_TILE_BLITS = 16_384;
    private static final double VIEWPORT_COVER = 0.75;
    private static final int REGION_CHUNKS = 32;

    public static int viewportTiles(int tilePixels, int viewportPixels) {
        return tilePixels <= 0 ? 1 : (int) Math.ceil((double) viewportPixels / tilePixels) + 1;
    }

    // A looped axis paints the world's own regions and nothing more, however many empty tiles the viewport spans.
    public static int tilesWithContent(ToroidalShape shape, int tilePixels, int viewWidth, int viewHeight) {
        if (shape == null) {
            return 1;
        }

        return tilesAlong(shape, Direction.Axis.X, tilePixels, viewWidth)
                * tilesAlong(shape, Direction.Axis.Z, tilePixels, viewHeight);
    }

    public static int regionSpan(ToroidalShape shape, Direction.Axis axis) {
        if (!shape.loops(axis)) {
            return 0;
        }

        int minRegion = Math.floorDiv(shape.minChunk(axis), REGION_CHUNKS);
        int maxRegion = Math.floorDiv(shape.maxChunk(axis) - 1, REGION_CHUNKS);
        return maxRegion - minRegion + 1;
    }

    private static int tilesAlong(ToroidalShape shape, Direction.Axis axis, int tilePixels, int viewportPixels) {
        return shape.loops(axis) ? regionSpan(shape, axis) : viewportTiles(tilePixels, viewportPixels);
    }

    public static int copyRangeCap(int loopedAxes, int tilesWithContent) {
        int budget = MAX_TILE_BLITS / Math.max(1, tilesWithContent);
        return switch (loopedAxes) {
            case 2 -> (int) ((Math.sqrt(budget) - 1) / 2);
            case 1 -> (budget - 1) / 2;
            default -> 0;
        };
    }

    public static int copyRange(int loopedAxes, int tilesWithContent, double periodPixels, int viewportPixels) {
        return Math.min(copiesToCover(periodPixels, viewportPixels), copyRangeCap(loopedAxes, tilesWithContent));
    }

    public static Copies painted(AxisCopies x, int rangeX, AxisCopies z, int rangeZ) {
        return new Copies(Math.max(rangeX, rangeZ), new BoundingBox(
                paintedMin(x, rangeX), Integer.MIN_VALUE, paintedMin(z, rangeZ),
                paintedMax(x, rangeX), Integer.MAX_VALUE, paintedMax(z, rangeZ)));
    }

    private static int copiesToCover(double periodPixels, int viewportPixels) {
        return periodPixels <= 0.0 ? 0 : (int) Math.ceil(viewportPixels * VIEWPORT_COVER / periodPixels);
    }

    private static int paintedMin(AxisCopies copies, int range) {
        return copies.loops() ? copies.min() + copies.offset(-range) : Integer.MIN_VALUE;
    }

    private static int paintedMax(AxisCopies copies, int range) {
        return copies.loops() ? copies.max() + copies.offset(range) - 1 : Integer.MAX_VALUE;
    }

    private MapCopyBudget() {
    }
}
