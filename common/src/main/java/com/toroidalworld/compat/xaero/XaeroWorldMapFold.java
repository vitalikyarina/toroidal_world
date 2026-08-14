package com.toroidalworld.compat.xaero;

import java.util.ArrayList;
import java.util.List;

import com.toroidalworld.api.ToroidalShape;
import com.toroidalworld.api.ToroidalWorldClientApi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

// The bridge the Xaero's World Map mixins talk to. The world map persists regions keyed by the client's mirror
// coordinates and derives its multiworld identity from the held spawn — both fold to the canonical copy here, so
// the stored world is the canonical one and a lap stops minting fresh strips and fresh waypoint worlds. The
// camera-follow anchor and the element reads fold canonical too: the map shows the canonical world once, and
// everything on it (player arrow, waypoints, tracked players) sits at its canonical spot. Everything is the
// identity when the level has no shape, and the class carries no Xaero types — it is safe to load with the world
// map absent.
//
// The shape is re-resolved per call rather than cached: the synced bounds can arrive or change after the level
// exists, and a cached adapter would keep answering with the transformer it was built on.
public final class XaeroWorldMapFold {
    // One world-map tile chunk is 4 chunks (64 blocks); a region is 8 tile chunks (512 blocks).
    private static final int TILE_CHUNK_CHUNKS = 4;
    private static final int REGION_TILE_CHUNKS = 8;

    private static ToroidalShape shape() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? null : ToroidalWorldClientApi.shapeOf(level).orElse(null);
    }

    public static boolean active() {
        return shape() != null;
    }

    // The spawn the "mw" multiworld id is derived from, folded canonical — the same fix the minimap side has: the
    // anchor sync legitimately moves the held spawn a world width per lap, and the id must not follow.
    public static BlockPos foldIdSpawn(BlockPos spawn) {
        ToroidalShape shape = shape();
        if (shape == null || spawn == null) {
            return spawn;
        }

        BlockPos folded = shape.fold(spawn);
        return folded.getX() == spawn.getX() && folded.getZ() == spawn.getZ() ? spawn : folded;
    }

    // A storage tile chunk (64 blocks) folded canonical. Every world width is a multiple of a tile chunk
    // (MIN_CHUNK_WIDTH is 16 chunks), so the fold stays exact.
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

    // The canonical region indices covered by a mirror tile-chunk window. Regions cannot be folded directly — the
    // canonical world covers parts of two regions per axis (and the nether is smaller than one region) — so each
    // tile chunk folds on its own and the distinct regions are collected.
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

    // A view-space block coordinate folded canonical.
    public static int foldBlock(Direction.Axis axis, int coord) {
        ToroidalShape shape = shape();
        return shape == null ? coord : shape.foldBlock(axis, coord);
    }

    // Whether the full-map view can be glued at a given texture-slot size. Two alignment demands per looping
    // axis: the world width must be a multiple of the slot (the fold moves sources by whole widths), and the
    // world's edge must sit on the slot grid — otherwise every slot straddles the world edge internally and a
    // whole-slot substitution paints a fraction of the world per cell. At a zoom that fails either test the view
    // simply stays unglued.
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

    // The 3x3 cap, as on the JourneyMap side: the canonical world plus one copy per side. A slot farther out than
    // one world width on a looping axis stays blank.
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

    // The seam grid geometry for one axis: {first block inside the world, width in blocks}, or null when the axis
    // does not loop (no seam to draw along it).
    public static int[] seamBounds(Direction.Axis axis) {
        ToroidalShape shape = shape();
        if (shape == null || !shape.loops(axis)) {
            return null;
        }

        return new int[] {shape.minBlock(axis), shape.widthBlocks(axis)};
    }

    // The camera-follow anchor folded canonical: the map opens on the canonical world, where the folded storage
    // actually is. Only the follow path reads this — a freely panned camera is never folded.
    public static double foldCameraCoord(Direction.Axis axis, double coord) {
        ToroidalShape shape = shape();
        if (shape == null) {
            return coord;
        }

        return shape.foldCoord(axis, coord);
    }

    // An element render coordinate folded canonical — the map draws the canonical world once, so elements sit at
    // their canonical spot (nearest-copy belongs to the minimap, whose view is anchored to the player).
    public static double foldElementCoord(Direction.Axis axis, double coord) {
        ToroidalShape shape = shape();
        if (shape == null) {
            return coord;
        }

        return shape.foldCoord(axis, coord);
    }

    // A waypoint block coordinate folded canonical for the full-map wrapper — feeds the tooltip text, the sort
    // order and the render position derivation alike.
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
