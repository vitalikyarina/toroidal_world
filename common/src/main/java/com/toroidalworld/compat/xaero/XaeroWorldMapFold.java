package com.toroidalworld.compat.xaero;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.api.ToroidalWorldClientApi;
import com.toroidalworld.compat.AxisCopies;
import com.toroidalworld.core.LogRateGate;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class XaeroWorldMapFold {
    private static final Logger LOGGER = LogUtils.getLogger();

    // Xaero's own units: a tile chunk is 4 chunks (64 blocks), a region 8 tile chunks (512 blocks).
    private static final int TILE_CHUNK_CHUNKS = 4;
    private static final int REGION_TILE_CHUNKS = 8;

    private static final int TORUS_COPIES = 9;
    private static final int UNCLIPPED_COPIES = 1;
    private static final int TORUS_GRID_LINES = 8;

    private static final LogRateGate clipCopiesGate = new LogRateGate();
    private static final LogRateGate seamGridGate = new LogRateGate();
    private static String lastClipCopies = "";
    private static String lastSeamGrid = "";

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

    public static boolean withinOnePeriod(int viewBlockX, int foldedBlockX, int viewBlockZ, int foldedBlockZ) {
        ToroidalShape shape = shape();
        if (shape == null) {
            return false;
        }

        if (shape.loops(Direction.Axis.X) && Math.abs(viewBlockX - foldedBlockX) > shape.widthBlocks(Direction.Axis.X)) {
            return false;
        }

        return !shape.loops(Direction.Axis.Z) || Math.abs(viewBlockZ - foldedBlockZ) <= shape.widthBlocks(Direction.Axis.Z);
    }

    public static AxisCopies copies(Direction.Axis axis) {
        ToroidalShape shape = shape();
        return shape == null ? AxisCopies.UNBOUNDED : AxisCopies.of(shape, axis);
    }

    public static int[] gridLines(AxisCopies copies) {
        if (!copies.loops()) {
            return new int[0];
        }

        List<Integer> laps = copies.laps();
        int[] lines = new int[laps.size() + 1];
        for (int i = 0; i < laps.size(); i++) {
            lines[i] = copies.min() + copies.offset(laps.get(i));
        }

        lines[laps.size()] = copies.max() + copies.offset(laps.getLast());
        return lines;
    }

    public static int[] gridExtent(AxisCopies copies, double camera, int windowPixels, double scale, int margin) {
        if (copies.loops()) {
            List<Integer> laps = copies.laps();
            return new int[] {
                    copies.min() + copies.offset(laps.getFirst()),
                    copies.max() + copies.offset(laps.getLast())};
        }

        double halfSpan = windowPixels / 2.0 / scale;
        return new int[] {(int) Math.floor(camera - halfSpan) - margin, (int) Math.ceil(camera + halfSpan) + margin};
    }

    public static void logClipCopies(AxisCopies copiesX, AxisCopies copiesZ, int slotSize,
            int slotMinX, int slotMinZ, int clippedMinX, int clippedMaxX, int clippedMinZ, int clippedMaxZ) {
        String key = "slot_blocks=" + slotSize + " x_loops=" + copiesX.loops() + " z_loops=" + copiesZ.loops()
                + " x_laps=" + copiesX.laps().size() + " z_laps=" + copiesZ.laps().size()
                + " copies=" + copiesX.laps().size() * copiesZ.laps().size()
                + " legacy_copies=" + (copiesX.loops() && copiesZ.loops() ? TORUS_COPIES : UNCLIPPED_COPIES);
        if (key.equals(lastClipCopies) || !clipCopiesGate.tryPass()) {
            return;
        }

        lastClipCopies = key;
        LOGGER.info("[xaero-compat] clip_copies {} slot_x={} slot_z={} clip_x_min={} clip_x_max={} clip_z_min={} clip_z_max={}",
                key, slotMinX, slotMinZ, clippedMinX, clippedMaxX, clippedMinZ, clippedMaxZ);
    }

    public static void logSeamGrid(AxisCopies copiesX, AxisCopies copiesZ, int[] extentX, int[] extentZ) {
        int linesX = gridLines(copiesX).length;
        int linesZ = gridLines(copiesZ).length;
        String key = "x_loops=" + copiesX.loops() + " z_loops=" + copiesZ.loops()
                + " x_lines=" + linesX + " z_lines=" + linesZ + " lines=" + (linesX + linesZ)
                + " legacy_lines=" + (copiesX.loops() && copiesZ.loops() ? TORUS_GRID_LINES : 0);
        if (key.equals(lastSeamGrid) || !seamGridGate.tryPass()) {
            return;
        }

        lastSeamGrid = key;
        LOGGER.info("[xaero-compat] seam_grid {} x_extent_min={} x_extent_max={} z_extent_min={} z_extent_max={}",
                key, extentX[0], extentX[1], extentZ[0], extentZ[1]);
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
