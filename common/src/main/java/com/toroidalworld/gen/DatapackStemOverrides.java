package com.toroidalworld.gen;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;

final class DatapackStemOverrides {

    enum Outcome {
        RESHAPED,
        STAMPED,
        REFUSED
    }

    record StemOverride(Outcome outcome, String datapackGenerator) {
    }

    private static volatile Map<ResourceKey<LevelStem>, StemOverride> overrides = Map.of();

    static void replaceAll(Map<ResourceKey<LevelStem>, StemOverride> baked) {
        overrides = Map.copyOf(baked);
    }

    static @Nullable StemOverride of(ResourceKey<LevelStem> stem) {
        return overrides.get(stem);
    }

    private DatapackStemOverrides() {
    }
}
