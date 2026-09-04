package com.toroidalworld.client;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class PublishedShapes {
    private static volatile Map<ResourceKey<Level>, WorldFold> folds = Map.of();

    public static void publish(ResourceKey<Level> dimension, WorldFold fold) {
        Map<ResourceKey<Level>, WorldFold> next = new HashMap<>(folds);
        next.put(dimension, fold);
        folds = Map.copyOf(next);
    }

    public static @Nullable WorldFold foldOf(ResourceKey<Level> dimension) {
        WorldFold fold = folds.get(dimension);
        return fold != null && fold.isWrapped() ? fold : null;
    }

    public static void clear() {
        folds = Map.of();
    }

    private PublishedShapes() {
    }
}
