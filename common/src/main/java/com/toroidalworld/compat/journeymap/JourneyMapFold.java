package com.toroidalworld.compat.journeymap;

import java.io.File;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.client.engine.ClientFrame;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.compat.ClientShapes;
import com.toroidalworld.compat.FullscreenZoomFloor;
import com.toroidalworld.compat.MapCopyBudget;
import com.toroidalworld.engine.seam.MapSurfaceCopies.Copies;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public final class JourneyMapFold {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final int REGION_BLOCKS = 512;

    public static final String WORLD_CHANGED = "world";
    public static final String DIMENSION_CHANGED = "dimension";

    private static int fullscreenRangeX;
    private static int fullscreenRangeZ;

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
        return MapCopyBudget.tilesWithContent(ClientShapes.current(), zoom, viewportX, viewportZ);
    }

    public static void recordFullscreenCopyRange(int rangeX, int rangeZ) {
        fullscreenRangeX = rangeX;
        fullscreenRangeZ = rangeZ;
    }

    public static Copies fullscreenCopies() {
        return MapCopyBudget.painted(copies(Direction.Axis.X), fullscreenRangeX,
                copies(Direction.Axis.Z), fullscreenRangeZ);
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

        int span = Math.max(MapCopyBudget.regionSpan(shape, Direction.Axis.X),
                MapCopyBudget.regionSpan(shape, Direction.Axis.Z));
        return span == 0 ? 0 : 2 * span + 3;
    }

    private JourneyMapFold() {
    }
}
