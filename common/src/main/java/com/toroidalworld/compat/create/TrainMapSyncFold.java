package com.toroidalworld.compat.create;

import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class TrainMapSyncFold {
    private static final int FLOATS_PER_BOGEY = 3;
    private static final int BOGEYS_PER_CARRIAGE = 2;
    private static final int FLOATS_PER_CARRIAGE = FLOATS_PER_BOGEY * BOGEYS_PER_CARRIAGE;
    private static final int X_OFFSET = 0;
    private static final int Z_OFFSET = 2;

    public static void coherent(Float @Nullable [] positions, @Nullable List<ResourceKey<Level>> dimensions,
            Function<ResourceKey<Level>, @Nullable WorldLoopTransformer> transformers) {
        if (positions == null || dimensions == null || positions.length < FLOATS_PER_CARRIAGE) {
            return;
        }

        Float[] anchor = null;
        for (int carriage = 0; carriage * FLOATS_PER_CARRIAGE + FLOATS_PER_CARRIAGE <= positions.length; carriage++) {
            ResourceKey<Level> dimension = carriage < dimensions.size() ? dimensions.get(carriage) : null;
            WorldLoopTransformer transformer = dimension == null ? null : transformers.apply(dimension);
            if (transformer == null) {
                continue;
            }

            int base = carriage * FLOATS_PER_CARRIAGE;
            if (anchor == null) {
                anchor = new Float[] {positions[base + X_OFFSET], positions[base + Z_OFFSET]};
            } else {
                rebase(transformer, positions, base, anchor[0], anchor[1]);
            }

            rebase(transformer, positions, base + FLOATS_PER_BOGEY, positions[base + X_OFFSET],
                    positions[base + Z_OFFSET]);
        }
    }

    public static void rebaseOnto(Float @Nullable [] stale, Float @Nullable [] current,
            @Nullable List<ResourceKey<Level>> dimensions,
            Function<ResourceKey<Level>, @Nullable WorldLoopTransformer> transformers) {
        if (stale == null || current == null || dimensions == null) {
            return;
        }

        int shared = Math.min(stale.length, current.length);
        for (int carriage = 0; carriage * FLOATS_PER_CARRIAGE + FLOATS_PER_CARRIAGE <= shared; carriage++) {
            ResourceKey<Level> dimension = carriage < dimensions.size() ? dimensions.get(carriage) : null;
            WorldLoopTransformer transformer = dimension == null ? null : transformers.apply(dimension);
            if (transformer == null) {
                continue;
            }

            int base = carriage * FLOATS_PER_CARRIAGE;
            for (int bogey = 0; bogey < BOGEYS_PER_CARRIAGE; bogey++) {
                int at = base + bogey * FLOATS_PER_BOGEY;
                rebase(transformer, stale, at, current[at + X_OFFSET], current[at + Z_OFFSET]);
            }
        }
    }

    private static void rebase(WorldLoopTransformer transformer, Float[] positions, int at, float anchorX,
            float anchorZ) {
        float rawX = positions[at + X_OFFSET];
        float rawZ = positions[at + Z_OFFSET];
        float nearX = (float) nearest(transformer.coords.x, anchorX, rawX);
        float nearZ = (float) nearest(transformer.coords.z, anchorZ, rawZ);
        if (nearX == rawX && nearZ == rawZ) {
            return;
        }

        positions[at + X_OFFSET] = nearX;
        positions[at + Z_OFFSET] = nearZ;
    }

    private static double nearest(WrapDomain domain, double anchor, double coord) {
        return domain.unwrapAround(anchor, coord);
    }

    private TrainMapSyncFold() {
    }
}
