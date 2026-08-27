package com.toroidalworld.compat.create;

import java.util.List;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class TrainMapSyncFold {
    private static final int FLOATS_PER_BOGEY = 3;
    private static final int BOGEYS_PER_CARRIAGE = 2;
    private static final int FLOATS_PER_CARRIAGE = FLOATS_PER_BOGEY * BOGEYS_PER_CARRIAGE;
    private static final int X_OFFSET = 0;
    private static final int Z_OFFSET = 2;

    public static void coherent(Float @Nullable [] positions, @Nullable List<ResourceKey<Level>> dimensions,
            Function<ResourceKey<Level>, @Nullable WorldFold> transformers) {
        if (positions == null || dimensions == null || positions.length < FLOATS_PER_CARRIAGE) {
            return;
        }

        Float[] anchor = null;
        for (int carriage = 0; carriage * FLOATS_PER_CARRIAGE + FLOATS_PER_CARRIAGE <= positions.length; carriage++) {
            ResourceKey<Level> dimension = carriage < dimensions.size() ? dimensions.get(carriage) : null;
            WorldFold transformer = dimension == null ? null : transformers.apply(dimension);
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
            Function<ResourceKey<Level>, @Nullable WorldFold> transformers) {
        if (stale == null || current == null || dimensions == null) {
            return;
        }

        int shared = Math.min(stale.length, current.length);
        for (int carriage = 0; carriage * FLOATS_PER_CARRIAGE + FLOATS_PER_CARRIAGE <= shared; carriage++) {
            ResourceKey<Level> dimension = carriage < dimensions.size() ? dimensions.get(carriage) : null;
            WorldFold transformer = dimension == null ? null : transformers.apply(dimension);
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

    private static void rebase(WorldFold transformer, Float[] positions, int at, float anchorX,
            float anchorZ) {
        Vec3 raw = new Vec3(positions[at + X_OFFSET], 0.0, positions[at + Z_OFFSET]);
        Vec3 nearest = transformer.nearestCopy(new Vec3(anchorX, 0.0, anchorZ), raw);
        if (nearest == raw) {
            return;
        }

        positions[at + X_OFFSET] = (float) nearest.x;
        positions[at + Z_OFFSET] = (float) nearest.z;
    }

    private TrainMapSyncFold() {
    }
}
