package com.toroidalworld.core;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.phys.AABB;

public final class FoldedBoxQuery {
    public static List<AABB> pieces(@Nullable WorldFold fold, AABB box) {
        if (fold == null || !fold.isWrapped() || !fold.crossesBounds(box)) {
            return List.of(box);
        }

        List<WorldFold.Folded<AABB>> folded = fold.split(box);
        return folded.stream().map(WorldFold.Folded::value).toList();
    }

    public static <T> Consumer<T> deduplicating(Consumer<T> output) {
        Set<T> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        return value -> {
            if (seen.add(value)) {
                output.accept(value);
            }
        };
    }

    public static <T> AbortableIterationConsumer<T> deduplicating(AbortableIterationConsumer<T> output) {
        Set<T> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        return value -> seen.add(value)
                ? output.accept(value)
                : AbortableIterationConsumer.Continuation.CONTINUE;
    }

    private FoldedBoxQuery() {
    }
}
