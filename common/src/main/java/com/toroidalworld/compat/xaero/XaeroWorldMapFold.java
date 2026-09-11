package com.toroidalworld.compat.xaero;

import java.util.ArrayList;
import java.util.List;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.api.v1.ToroidalWorldClientApi;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.compat.ClientShapes;
import com.toroidalworld.compat.FullscreenZoomFloor;
import com.toroidalworld.core.CoordinateConstants;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.world.MapDimension;
import xaero.map.world.MapWorld;

public final class XaeroWorldMapFold {
    public static final int REGION_BLOCKS = 512;
    public static final int SLOT_BLOCKS = 64;

    private static final int TILE_CHUNK_CHUNKS = 4;
    private static final int REGION_TILE_CHUNKS = 8;

    private static final int COMPARISON_CHUNK_OFFSET = 16;

    private static ToroidalShape browsedShape() {
        WorldMapSession session = WorldMapSession.getCurrentSession();
        MapProcessor processor = session == null ? null : session.getMapProcessor();
        MapWorld mapWorld = processor == null ? null : processor.getMapWorld();
        MapDimension dimension = mapWorld == null ? null : mapWorld.getCurrentDimension();
        return dimension == null ? null : ToroidalWorldClientApi.shapeOf(dimension.getDimId()).orElse(null);
    }

    public static boolean active() {
        return browsedShape() != null;
    }

    public static BlockPos foldIdSpawn(ClientLevel level, BlockPos spawn) {
        ToroidalShape shape = ClientShapes.of(level);
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

    public static int firstTileChunkOfRegion(int region) {
        return region * REGION_TILE_CHUNKS;
    }

    public static int regionOfTileChunk(int tileChunk) {
        return Math.floorDiv(tileChunk, REGION_TILE_CHUNKS);
    }

    public static int tileChunkInRegion(int tileChunk) {
        return Math.floorMod(tileChunk, REGION_TILE_CHUNKS);
    }

    public static int foldRegion(Direction.Axis axis, int region) {
        return regionOfTileChunk(foldTileChunk(axis, firstTileChunkOfRegion(region)));
    }

    public static int foldChunk(Direction.Axis axis, int chunk) {
        ToroidalShape shape = browsedShape();
        return shape == null ? chunk : shape.foldChunk(axis, chunk);
    }

    public static int foldComparisonChunk(Direction.Axis axis, int comparison) {
        return foldChunk(axis, comparison + COMPARISON_CHUNK_OFFSET) - COMPARISON_CHUNK_OFFSET;
    }

    public static int[] canonicalRegions(Direction.Axis axis, int startTileChunk, int endTileChunk) {
        List<Integer> regions = new ArrayList<>();
        for (int tileChunk = startTileChunk; tileChunk <= endTileChunk; tileChunk++) {
            int region = regionOfTileChunk(foldTileChunk(axis, tileChunk));
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

        for (Direction.Axis axis : CoordinateConstants.HORIZONTAL_AXES) {
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

    public static AxisCopies chunkCopies(Direction.Axis axis) {
        ToroidalShape shape = browsedShape();
        return shape == null ? AxisCopies.UNBOUNDED : AxisCopies.ofChunks(shape, axis);
    }

    public static double zoomFloorScale(double scaleMultiplier) {
        ToroidalShape shape = browsedShape();
        return shape == null ? 0.0 : FullscreenZoomFloor.xaeroScale(shape, scaleMultiplier);
    }

    public static int[] viewSpan(double camera, int windowPixels, double scale, int margin) {
        double halfSpan = windowPixels / 2.0 / scale;
        return new int[] {(int) Math.floor(camera - halfSpan) - margin, (int) Math.ceil(camera + halfSpan) + margin};
    }

    public static double foldCoord(Direction.Axis axis, double coord) {
        ToroidalShape shape = browsedShape();
        return shape == null ? coord : shape.foldCoord(axis, coord);
    }

    public static double foldFootprintCoord(ClientLevel level, Direction.Axis axis, double coord) {
        ToroidalShape shape = ClientShapes.of(level);
        if (shape == null) {
            return coord;
        }

        return shape.foldCoord(axis, coord);
    }

    private XaeroWorldMapFold() {
    }
}
