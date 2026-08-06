package com.toroidalworld.options;

import com.toroidalworld.core.CoordinateConstants;

import net.minecraft.world.level.border.WorldBorder;

// Where a looped world still works, as opposed to where it is merely representable. The settings screen validates the
// typed size against these bounds — a number outside them holds Done back rather than being silently corrected.
public final class WorldLoopSizes {
    // The smallest view distance still worth playing at. Below this the world is technically fine and practically not.
    private static final int MIN_PLAYABLE_VIEW_DISTANCE = 5;

    // A wrapped world can only be rendered out to its radius minus a buffer (WorldLoopTransformer.maxViewDistance),
    // so the smallest world we offer is the one that still leaves a playable view distance after that subtraction —
    // any smaller and a player would see the same terrain twice, and eventually their own back.
    private static final int MIN_CHUNK_RADIUS = CoordinateConstants.VIEW_DISTANCE_MARGIN + MIN_PLAYABLE_VIEW_DISTANCE;

    // Vanilla runs out first: its own world border caps how far a coordinate may go — so there is no ceiling of ours
    // left to invent.
    private static final int MAX_CHUNK_RADIUS = (int) (WorldBorder.MAX_CENTER_COORDINATE / CoordinateConstants.CHUNK_WIDTH);

    public static final int MIN_CHUNK_WIDTH = MIN_CHUNK_RADIUS * 2;
    public static final int MAX_CHUNK_WIDTH = MAX_CHUNK_RADIUS * 2;

    // The End's floor is progression, not playability: outer islands only generate in cells further than 64 chunks
    // (1024 blocks) from the origin — any world without them has no end cities, no elytra, no shulkers, no chorus.
    // 192 chunks (3072 blocks) keeps a solid island ring and keeps the gateway teleport reach (1024 + 256 blocks)
    // inside the minimal half-width (1536 blocks), so gateway targets cannot cross the seam.
    public static final int END_MIN_CHUNK_WIDTH = 192;
    public static final int END_DEFAULT_CHUNK_WIDTH = 256;

    public static boolean isInRange(int chunkWidth) {
        return chunkWidth >= MIN_CHUNK_WIDTH && chunkWidth <= MAX_CHUNK_WIDTH;
    }

    public static boolean isEndInRange(int chunkWidth) {
        return chunkWidth >= END_MIN_CHUNK_WIDTH && chunkWidth <= MAX_CHUNK_WIDTH;
    }

    private WorldLoopSizes() {
    }
}
