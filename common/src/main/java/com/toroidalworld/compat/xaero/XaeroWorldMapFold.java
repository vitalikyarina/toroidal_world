package com.toroidalworld.compat.xaero;

import java.util.ArrayList;
import java.util.List;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.api.ToroidalWorldClientApi;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.compat.FullscreenZoomFloor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class XaeroWorldMapFold {
    // Xaero's own units: a tile chunk is 4 chunks (64 blocks), a region 8 tile chunks (512 blocks).
    private static final int TILE_CHUNK_CHUNKS = 4;
    private static final int REGION_TILE_CHUNKS = 8;

    private static ToroidalShape shape() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? null : ToroidalWorldClientApi.shapeOf(level).orElse(null);
    }

    public static boolean active() {
        return shape() != null;
    }

    public static BlockPos foldIdSpawn(BlockPos spawn) {
        ToroidalShape shape = shape();
        if (shape == null || spawn == null) {
            return spawn;
        }

        BlockPos folded = shape.fold(spawn);
        return folded.getX() == spawn.getX() && folded.getZ() == spawn.getZ() ? spawn : folded;
    }

    public static int foldTileChunk(Direction.Axis axis, int tileChunk) {
        ToroidalShape shape = shape();
        if (shape == null) {
            return tileChunk;
        }

        return Math.floorDiv(shape.foldChunk(axis, tileChunk * TILE_CHUNK_CHUNKS), TILE_CHUNK_CHUNKS);
    }

    public static int foldChunk(Direction.Axis axis, int chunk) {
        ToroidalShape shape = shape();
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
        ToroidalShape shape = shape();
        return shape == null ? coord : shape.foldBlock(axis, coord);
    }

    public static boolean glueableAt(int slotSizeBlocks) {
        ToroidalShape shape = shape();
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
        ToroidalShape shape = shape();
        return shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, axis);
    }

    public static double zoomFloorScale(double scaleMultiplier) {
        ToroidalShape shape = shape();
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
        ToroidalShape shape = shape();
        if (shape == null) {
            return coord;
        }

        return shape.foldCoord(axis, coord);
    }

    public static double foldElementCoord(Direction.Axis axis, double coord) {
        ToroidalShape shape = shape();
        if (shape == null) {
            return coord;
        }

        return shape.foldCoord(axis, coord);
    }

    public static double foldFootprintCoord(Direction.Axis axis, double coord) {
        ToroidalShape shape = shape();
        if (shape == null) {
            return coord;
        }

        return shape.foldCoord(axis, coord);
    }

    public static int foldWaypointBlock(Direction.Axis axis, int coord) {
        ToroidalShape shape = shape();
        if (shape == null) {
            return coord;
        }

        return shape.foldBlock(axis, coord);
    }

    private XaeroWorldMapFold() {
    }
}
