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

    private static final LogRateGate capGate = new LogRateGate();

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

    public static int copyRange(double periodPixels, int viewportPixels) {
        if (periodPixels <= 0.0) {
            return 0;
        }

        int needed = (int) Math.ceil(viewportPixels * 0.75 / periodPixels);
        if (needed > COPY_RANGE_CAP) {
            if (capGate.tryPass()) {
                LOGGER.info("[jm-compat] tile_copies capped needed={} cap={}", needed, COPY_RANGE_CAP);
            }
            return COPY_RANGE_CAP;
        }

        return needed;
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
