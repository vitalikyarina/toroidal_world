package com.toroidalworld.compat.journeymap;

import org.slf4j.Logger;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.api.ToroidalWorldClientApi;
import com.toroidalworld.core.LogRateGate;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class JourneyMapFold {
    private static final Logger LOGGER = LogUtils.getLogger();

    // JourneyMap's own unit: one region tile is 512 blocks of ground.
    private static final double REGION_BLOCKS = 512.0;

    private static final int COPY_RANGE_CAP = 5;
    private static final int MAX_TILE_COPIES = (2 * COPY_RANGE_CAP + 1) * (2 * COPY_RANGE_CAP + 1);
    private static final int ONE_AXIS_COPY_RANGE_CAP = (MAX_TILE_COPIES - 1) / 2;
    private static final double VIEWPORT_COVER = 0.75;

    private static final LogRateGate tileCopiesGate = new LogRateGate();
    private static String lastTileCopies = "";

    private static ToroidalShape shape() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? null : ToroidalWorldClientApi.shapeOf(level).orElse(null);
    }

    public static int foldRegionChunk(Direction.Axis axis, int chunk) {
        ToroidalShape shape = shape();
        return shape == null ? chunk : shape.foldChunk(axis, chunk);
    }

    public static double foldCenterCoord(Direction.Axis axis, double coord) {
        ToroidalShape shape = shape();
        return shape == null ? coord : shape.foldCoord(axis, coord);
    }

    public static double nearestPixelCoord(Direction.Axis axis, double ref, double coord) {
        ToroidalShape shape = shape();
        return shape == null ? coord : shape.nearestCoord(axis, ref, coord);
    }

    public static Vec3 nearestToPlayer(Vec3 position) {
        ToroidalShape shape = shape();
        if (shape == null || position == null) {
            return position;
        }

        var player = Minecraft.getInstance().player;
        return player == null ? position : shape.nearestCopy(player.position(), position);
    }

    public static boolean active() {
        return shape() != null;
    }

    public static int foldUiCoord(Direction.Axis axis, int coord) {
        ToroidalShape shape = shape();
        return shape == null ? coord : shape.foldBlock(axis, coord);
    }

    public static BlockPos foldUiBlock(BlockPos pos) {
        ToroidalShape shape = shape();
        return shape == null || pos == null ? pos : shape.fold(pos);
    }

    public static double worldPixelPeriod(Direction.Axis axis, int zoom) {
        ToroidalShape shape = shape();
        if (shape == null || !shape.loops(axis)) {
            return 0.0;
        }

        return shape.widthBlocks(axis) * (zoom / REGION_BLOCKS);
    }

    public static int loopedAxes() {
        ToroidalShape shape = shape();
        if (shape == null) {
            return 0;
        }

        return (shape.loops(Direction.Axis.X) ? 1 : 0) + (shape.loops(Direction.Axis.Z) ? 1 : 0);
    }

    public static int copyRangeCap(int loopedAxes) {
        return loopedAxes == 2 ? COPY_RANGE_CAP : ONE_AXIS_COPY_RANGE_CAP;
    }

    public static int copyRange(int loopedAxes, double periodPixels, int viewportPixels) {
        return Math.min(copiesToCover(periodPixels, viewportPixels), copyRangeCap(loopedAxes));
    }

    private static int copiesToCover(double periodPixels, int viewportPixels) {
        return periodPixels <= 0.0 ? 0 : (int) Math.ceil(viewportPixels * VIEWPORT_COVER / periodPixels);
    }

    public static void logTileCopies(String context, int zoom, int loopedAxes, double periodX, double periodZ,
            int viewportX, int viewportZ, int legacyViewportX, int legacyViewportZ) {
        String line = "context=" + context + " looped_axes=" + loopedAxes + " zoom_px=" + zoom
                + " period_x_px=" + periodX + " period_z_px=" + periodZ
                + " viewport_x_px=" + viewportX + " viewport_z_px=" + viewportZ
                + " legacy_viewport_x_px=" + legacyViewportX + " legacy_viewport_z_px=" + legacyViewportZ
                + " needed_x=" + copiesToCover(periodX, viewportX) + " needed_z=" + copiesToCover(periodZ, viewportZ)
                + " range_x=" + copyRange(loopedAxes, periodX, viewportX)
                + " range_z=" + copyRange(loopedAxes, periodZ, viewportZ)
                + " legacy_range_x=" + Math.min(copiesToCover(periodX, legacyViewportX), COPY_RANGE_CAP)
                + " legacy_range_z=" + Math.min(copiesToCover(periodZ, legacyViewportZ), COPY_RANGE_CAP)
                + " cap=" + copyRangeCap(loopedAxes);
        if (line.equals(lastTileCopies) || !tileCopiesGate.tryPass()) {
            return;
        }

        lastTileCopies = line;
        LOGGER.info("[jm-compat] tile_copies {}", line);
    }

    public static void gridDropped(String fromDimension, String toDimension) {
        LOGGER.info("[jm-compat] grid_dropped from={} to={}", fromDimension, toDimension);
    }

    public static int minGridSize() {
        ToroidalShape shape = shape();
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

        int minRegion = Math.floorDiv(shape.minChunk(axis), 32);
        int maxRegion = Math.floorDiv(shape.maxChunk(axis) - 1, 32);
        return maxRegion - minRegion + 1;
    }

    private JourneyMapFold() {
    }
}
