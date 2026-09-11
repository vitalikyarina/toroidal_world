package com.toroidalworld.compat.journeymap;

import java.awt.geom.Rectangle2D;
import java.io.File;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.client.engine.ClientFrame;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.compat.ClientShapes;
import com.toroidalworld.compat.FullscreenZoomFloor;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

import journeymap.api.v2.common.Context;

public final class JourneyMapFold {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int REGION_BLOCKS = 512;
    private static final int REGION_CHUNKS = 32;

    private static final int MAX_TILE_BLITS = 16_384;
    private static final double VIEWPORT_COVER = 0.75;

    public static final String WORLD_CHANGED = "world";
    public static final String DIMENSION_CHANGED = "dimension";

    private static int fullscreenRangeX;
    private static int fullscreenRangeZ;
    private static int minimapRangeX;
    private static int minimapRangeZ;

    public static int foldRegionChunk(Direction.Axis axis, int chunk) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? chunk : shape.foldChunk(axis, chunk);
    }

    public static double foldCenterCoord(Direction.Axis axis, double coord) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? coord : shape.foldCoord(axis, coord);
    }

    public static double nearestPixelCoord(Direction.Axis axis, double ref, double coord) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? coord : shape.nearestCoord(axis, ref, coord);
    }

    public static Vec3 nearestToPlayer(Vec3 position) {
        return ClientFrame.nearestToPlayer(position);
    }

    public static boolean active() {
        return ClientShapes.current() != null;
    }

    public static int foldUiCoord(Direction.Axis axis, int coord) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? coord : shape.foldBlock(axis, coord);
    }

    public static BlockPos foldUiBlock(BlockPos pos) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null || pos == null ? pos : shape.fold(pos);
    }

    public static AxisCopies copies(Direction.Axis axis) {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, axis);
    }

    public static double worldPixelPeriod(Direction.Axis axis, int zoom) {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null || !shape.loops(axis)) {
            return 0.0;
        }

        return shape.widthBlocks(axis) * (zoom / (double) REGION_BLOCKS);
    }

    public static int loopedAxes() {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null) {
            return 0;
        }

        return (shape.loops(Direction.Axis.X) ? 1 : 0) + (shape.loops(Direction.Axis.Z) ? 1 : 0);
    }

    public static int zoomFloor() {
        ToroidalShape shape = ClientShapes.current();
        return shape == null ? 0 : FullscreenZoomFloor.journeyMapZoom(shape);
    }

    public static int[] viewSpan(double centerBlock, int windowPixels, int zoom) {
        double halfSpanBlocks = windowPixels / 2.0 * REGION_BLOCKS / zoom;
        return new int[] {(int) Math.floor(centerBlock - halfSpanBlocks), (int) Math.ceil(centerBlock + halfSpanBlocks)};
    }

    public static int tilesWithContent(int zoom, int viewportX, int viewportZ) {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null) {
            return 1;
        }

        return tilesAlong(shape, Direction.Axis.X, zoom, viewportX) * tilesAlong(shape, Direction.Axis.Z, zoom, viewportZ);
    }

    private static int tilesAlong(ToroidalShape shape, Direction.Axis axis, int zoom, int viewportPixels) {
        return shape.loops(axis) ? regionSpan(shape, axis) : viewportTiles(zoom, viewportPixels);
    }

    public static int viewportTiles(int zoom, int viewportPixels) {
        return zoom <= 0 ? 1 : (int) Math.ceil((double) viewportPixels / zoom) + 1;
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

    private static int copiesToCover(double periodPixels, int viewportPixels) {
        return periodPixels <= 0.0 ? 0 : (int) Math.ceil(viewportPixels * VIEWPORT_COVER / periodPixels);
    }

    public static void recordCopyRange(Context.UI ui, int rangeX, int rangeZ) {
        if (ui == Context.UI.Fullscreen) {
            fullscreenRangeX = rangeX;
            fullscreenRangeZ = rangeZ;
        } else if (ui == Context.UI.Minimap) {
            minimapRangeX = rangeX;
            minimapRangeZ = rangeZ;
        }
    }

    public static double[][] copyOffsets(Context.UI ui, int zoom, Rectangle2D.Double bounds, Rectangle2D.Double screen) {
        int rangeX = ui == Context.UI.Fullscreen ? fullscreenRangeX : ui == Context.UI.Minimap ? minimapRangeX : 0;
        int rangeZ = ui == Context.UI.Fullscreen ? fullscreenRangeZ : ui == Context.UI.Minimap ? minimapRangeZ : 0;
        return copyOffsets(rangeX, rangeZ, worldPixelPeriod(Direction.Axis.X, zoom),
                worldPixelPeriod(Direction.Axis.Z, zoom), bounds, screen);
    }

    static double[][] copyOffsets(int rangeX, int rangeZ, double periodX, double periodZ, Rectangle2D.Double bounds,
            Rectangle2D.Double screen) {
        int[] lapsX = visibleLaps(rangeX, periodX, bounds.getMinX(), bounds.getMaxX(), screen.getMinX(), screen.getMaxX());
        int[] lapsZ = visibleLaps(rangeZ, periodZ, bounds.getMinY(), bounds.getMaxY(), screen.getMinY(), screen.getMaxY());
        double[][] offsets = new double[lapsX.length * lapsZ.length][];
        int i = 0;
        for (int lapX : lapsX) {
            for (int lapZ : lapsZ) {
                offsets[i++] = new double[] {lapX * periodX, lapZ * periodZ};
            }
        }

        return offsets;
    }

    public static double[][] nearestCopyOffset(double[][] offsets, Rectangle2D.Double bounds, Rectangle2D.Double screen) {
        if (offsets.length <= 1) {
            return offsets;
        }

        double[] nearest = offsets[0];
        double nearestDistance = Double.MAX_VALUE;
        for (double[] offset : offsets) {
            double dx = bounds.getCenterX() + offset[0] - screen.getCenterX();
            double dz = bounds.getCenterY() + offset[1] - screen.getCenterY();
            double distance = dx * dx + dz * dz;
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = offset;
            }
        }

        return new double[][] {nearest};
    }

    private static int[] visibleLaps(int range, double period, double min, double max, double screenMin, double screenMax) {
        if (period <= 0.0) {
            return max >= screenMin && min <= screenMax ? new int[] {0} : new int[0];
        }

        int first = Math.max(-range, (int) Math.ceil((screenMin - max) / period));
        int last = Math.min(range, (int) Math.floor((screenMax - min) / period));
        if (last < first) {
            return new int[0];
        }

        int[] laps = new int[last - first + 1];
        for (int i = 0; i < laps.length; i++) {
            laps[i] = first + i;
        }

        return laps;
    }

    public static <D> @Nullable String staleGridReason(@Nullable D lastDimension, @Nullable D dimension,
            @Nullable File lastWorldDir, @Nullable File worldDir) {
        if (lastWorldDir != null && worldDir != null && !lastWorldDir.equals(worldDir)) {
            return WORLD_CHANGED;
        }

        if (lastDimension != null && !lastDimension.equals(dimension)) {
            return DIMENSION_CHANGED;
        }

        return null;
    }

    public static void gridDropped(String reason, String from, String to, int tilesDropped) {
        LOGGER.info("[jm-compat] grid_dropped reason={} from={} to={} tiles_dropped={}",
                reason, from.replace(' ', '_'), to.replace(' ', '_'), tilesDropped);
    }

    public static int minGridSize() {
        ToroidalShape shape = ClientShapes.current();
        if (shape == null) {
            return 0;
        }

        int span = Math.max(regionSpan(shape, Direction.Axis.X), regionSpan(shape, Direction.Axis.Z));
        return span == 0 ? 0 : 2 * span + 3;
    }

    private static int regionSpan(ToroidalShape shape, Direction.Axis axis) {
        if (!shape.loops(axis)) {
            return 0;
        }

        int minRegion = Math.floorDiv(shape.minChunk(axis), REGION_CHUNKS);
        int maxRegion = Math.floorDiv(shape.maxChunk(axis) - 1, REGION_CHUNKS);
        return maxRegion - minRegion + 1;
    }

    private JourneyMapFold() {
    }
}
