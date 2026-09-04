package com.toroidalworld.compat.xaero;

import java.util.ArrayList;
import java.util.List;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.api.ToroidalWorldClientApi;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.compat.FullscreenZoomFloor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

public final class XaeroWorldMapFold {
    // Xaero's own units: a tile chunk is 4 chunks (64 blocks), a region 8 tile chunks (512 blocks).
    private static final int TILE_CHUNK_CHUNKS = 4;
    private static final int REGION_TILE_CHUNKS = 8;

    private static ToroidalShape browsedShape() {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        MapProcessor processor = session == null ? null : session.getMapProcessor();
        MapWorld mapWorld = processor == null ? null : processor.getMapWorld();
        MapDimension dimension = mapWorld == null ? null : mapWorld.getCurrentDimension();
        return dimension == null ? null : ToroidalWorldClientApi.shapeOf(dimension.getDimId()).orElse(null);
    }

    private static ToroidalShape shapeOf(ClientLevel level) {
        return level == null ? null : ToroidalWorldClientApi.shapeOf(level).orElse(null);
    }

    public static boolean active() {
        return browsedShape() != null;
    }

    public static BlockPos foldIdSpawn(ClientLevel level, BlockPos spawn) {
        ToroidalShape shape = shapeOf(level);
        if (shape == null || spawn == null) {
            return spawn;
        }

        return shape.fold(spawn);
    }

    public static int foldTileChunk(Direction.Axis axis, int tileChunk) {
        ToroidalShape shape = browsedShape();
        if (shape == null) {
            return tileChunk;
        }

        return Math.floorDiv(shape.foldChunk(axis, tileChunk * TILE_CHUNK_CHUNKS), TILE_CHUNK_CHUNKS);
    }

    public static int foldChunk(Direction.Axis axis, int chunk) {
        ToroidalShape shape = browsedShape();
        return shape == null ? chunk : shape.foldChunk(axis, chunk);
    }

    public static int[] canonicalRegions(Direction.Axis axis, int startTileChunk, int endTileChunk) {
        List<Integer> regions = new ArrayList<>();
        for (int tileChunk = startTileChunk; tileChunk <= endTileChunk; tileChunk++) {
            int region = Math.floorDiv(foldTileChunk(axis, tileChunk), REGION_TILE_CHUNKS);
            if (!regions.contains(region)) {
                regions.add(region);
            }
        }

        int[] result = new int[regions.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = regions.get(i);
        }

        return result;
    }

    public static int foldBlock(Direction.Axis axis, int coord) {
        ToroidalShape shape = browsedShape();
        return shape == null ? coord : shape.foldBlock(axis, coord);
    }

    public static boolean glueableAt(int slotSizeBlocks) {
        ToroidalShape shape = browsedShape();
        if (shape == null) {
            return false;
        }

        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            if (shape.loops(axis)
                    && (shape.widthBlocks(axis) % slotSizeBlocks != 0
                            || Math.floorMod(shape.minBlock(axis), slotSizeBlocks) != 0)) {
                return false;
            }
        }

        return true;
    }

    public static AxisCopies copies(Direction.Axis axis) {
        ToroidalShape shape = browsedShape();
        return shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, axis);
    }

    public static double zoomFloorScale(double scaleMultiplier) {
        ToroidalShape shape = browsedShape();
        if (shape == null) {
            return 0.0;
        }

        double floor = 0.0;
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            if (shape.loops(axis)) {
                floor = Math.max(floor, FullscreenZoomFloor.xaeroScale(shape.widthBlocks(axis), scaleMultiplier));
            }
        }

        return floor;
    }

    public static int[] viewSpan(double camera, int windowPixels, double scale, int margin) {
        double halfSpan = windowPixels / 2.0 / scale;
        return new int[] {(int) Math.floor(camera - halfSpan) - margin, (int) Math.ceil(camera + halfSpan) + margin};
    }

    public static double foldCameraCoord(Direction.Axis axis, double coord) {
        ToroidalShape shape = browsedShape();
        if (shape == null) {
            return coord;
        }

        return shape.foldCoord(axis, coord);
    }

    public static double foldElementCoord(Direction.Axis axis, double coord) {
        ToroidalShape shape = browsedShape();
        if (shape == null) {
            return coord;
        }

        return shape.foldCoord(axis, coord);
    }

    public static double foldFootprintCoord(ClientLevel level, Direction.Axis axis, double coord) {
        ToroidalShape shape = shapeOf(level);
        if (shape == null) {
            return coord;
        }

        return shape.foldCoord(axis, coord);
    }

    public static int foldWaypointBlock(Direction.Axis axis, int coord) {
        ToroidalShape shape = browsedShape();
        if (shape == null) {
            return coord;
        }

        return shape.foldBlock(axis, coord);
    }

    private XaeroWorldMapFold() {
    }
}
