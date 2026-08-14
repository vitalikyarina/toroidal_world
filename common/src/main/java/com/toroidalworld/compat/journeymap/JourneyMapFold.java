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

// The one bridge the JourneyMap mixins talk to, and the mod's first consumer of its own public API. JourneyMap reads
// the client level and the raw player position, which near the seam run whole world widths from the server's truth —
// so its region keys are folded into the world bounds (the map returns to its start instead of growing a strip per
// lap) and its render reads are taken to the copy nearest the reference (a waypoint across the seam measures the
// short way). On an unwrapped level the shape is absent and every operation is the identity, byte-for-byte.
//
// The shape is re-resolved per call rather than cached: the synced bounds can arrive or change after the level
// exists, and a cached adapter would keep answering with the transformer it was built on.
public final class JourneyMapFold {
    private static final Logger LOGGER = LogUtils.getLogger();

    // One region tile is 512 blocks of ground drawn zoom pixels wide, so this is the px-per-block scale divisor.
    private static final double REGION_BLOCKS = 512.0;

    // Hard ceiling on wrapped copies per axis per side. An extreme zoom-out would otherwise ask for hundreds of
    // re-renders per tile; past the cap the far copies just stay undrawn, and the gated line below says so.
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

    // The waypoint read folded toward the local player. A null player only happens between levels, where nothing is
    // rendered from these getters anyway — identity keeps the call harmless.
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

    // Display-level folds: the location bars and the mouse-hover block.
    public static int foldUiCoord(Direction.Axis axis, int coord) {
        ToroidalShape shape = shape();
        return shape == null ? coord : shape.foldBlock(axis, coord);
    }

    public static BlockPos foldUiBlock(BlockPos pos) {
        ToroidalShape shape = shape();
        return shape == null || pos == null ? pos : shape.fold(pos);
    }

    // The world's width in map pixels at this zoom — the period wrapped tile copies repeat at. 0 on an axis that
    // does not loop (or with no shape at all), which the tile mixin reads as "no copies on this axis".
    public static double worldPixelPeriod(Direction.Axis axis, int zoom) {
        ToroidalShape shape = shape();
        if (shape == null || !shape.loops(axis)) {
            return 0.0;
        }

        return shape.widthBlocks(axis) * (zoom / REGION_BLOCKS);
    }

    // How many wrapped copies per side cover the viewport. 0.75 of the viewport rather than half: the minimap
    // rotates with the player, so coverage has to reach the half-diagonal, not the half-width.
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

    // The smallest tile ring that always holds the whole canonical world. JourneyMap keeps tiles only inside a
    // viewport-sized ring, but the glued copies render FROM those tiles — so a far-side region falling out of the
    // ring takes its copies with it (visible in the End: 8x8 regions, a zoomed-in ring loses the far edge). A
    // wrapped world is finite, so pinning the ring to its region span is a bounded cost: 4 tiles for the default
    // overworld, 64 for the default End. Returns 0 when nothing wraps; the ring stays odd by construction.
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
