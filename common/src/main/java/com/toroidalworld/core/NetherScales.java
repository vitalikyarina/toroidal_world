package com.toroidalworld.core;

import java.util.ArrayList;
import java.util.List;

public final class NetherScales {
    public static final int DEFAULT = 8;

    private static final int SMALLEST = 1;

    public static List<Integer> allowedFor(int overworldChunkWidth) {
        int maxScale = overworldChunkWidth / WorldLoopSizes.MIN_CHUNK_WIDTH;
        if (maxScale < SMALLEST) {
            return List.of(SMALLEST);
        }

        List<Integer> scales = new ArrayList<>();
        for (int divisor : Divisors.of(overworldChunkWidth)) {
            if (divisor > maxScale) {
                break;
            }

            scales.add(divisor);
        }

        return scales;
    }

    public static int normalize(int scale, int overworldChunkWidth) {
        return normalize(scale, allowedFor(overworldChunkWidth));
    }

    public static int normalize(int scale, List<Integer> allowed) {
        int fallen = allowed.get(0);
        for (int candidate : allowed) {
            if (candidate > scale) {
                break;
            }

            fallen = candidate;
        }

        return fallen;
    }

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
