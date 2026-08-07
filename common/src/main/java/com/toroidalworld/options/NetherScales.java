package com.toroidalworld.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Vanilla's 8:1 is not a size but the portal mapping: a portal at overworld x links to nether x/scale. On a torus that
// forces overworldWidth / scale == netherWidth exactly — break it and a portal near the seam maps to a coordinate
// outside the nether, which is where the wrapping matters most. So a scale is usable only when it divides the world
// width evenly and leaves a nether that is itself a working looped world.
//
// The consequence is worth knowing before reading any caller: at the smallest world size only 1:1 is possible, and 1:8
// needs a world at least eight times the minimum.
public final class NetherScales {
    public static final int DEFAULT = 8;

    private static final int SMALLEST = 1;

    public static List<Integer> allowedFor(int overworldChunkWidth) {
        int maxScale = overworldChunkWidth / WorldLoopSizes.MIN_CHUNK_WIDTH;
        if (maxScale < SMALLEST) {
            return List.of(SMALLEST);
        }

        // Divisors come in pairs around the square root, so both ends of each pair are collected in one pass rather than
        // walking every integer up to the width — which, at the sizes this screen allows, would be millions of them.
        List<Integer> scales = new ArrayList<>();
        for (int candidate = 1; (long) candidate * candidate <= overworldChunkWidth; candidate++) {
            if (overworldChunkWidth % candidate != 0) {
                continue;
            }

            if (candidate <= maxScale) {
                scales.add(candidate);
            }

            int paired = overworldChunkWidth / candidate;
            if (paired != candidate && paired <= maxScale) {
                scales.add(paired);
            }
        }

        Collections.sort(scales);
        return scales;
    }

    // Ties go to the larger scale, so a world that grows past a threshold moves toward vanilla's 8:1 rather than away.
    public static int normalize(int scale, int overworldChunkWidth) {
        return normalize(scale, allowedFor(overworldChunkWidth));
    }

    // For a caller that already has the width's allowed list — the settings screen fetches it once and derives both the
    // normalized scale and the cycle button's state from it, rather than rebuilding the divisors twice.
    public static int normalize(int scale, List<Integer> allowed) {
        int nearest = SMALLEST;
        for (int candidate : allowed) {
            if (Math.abs(candidate - scale) <= Math.abs(nearest - scale)) {
                nearest = candidate;
            }
        }

        return nearest;
    }

    // The scale handed in is already one of the allowed ones (its only caller cycles a normalized value), so it is found
    // directly — no re-normalize. A value that is not in the list gives indexOf -1, which advances to element 0.
    public static int next(int scale, int overworldChunkWidth) {
        List<Integer> allowed = allowedFor(overworldChunkWidth);
        int index = allowed.indexOf(scale);
        return allowed.get((index + 1) % allowed.size());
    }

    public static int netherChunkWidth(int overworldChunkWidth, int scale) {
        return overworldChunkWidth / scale;
    }

    private NetherScales() {
    }
}
