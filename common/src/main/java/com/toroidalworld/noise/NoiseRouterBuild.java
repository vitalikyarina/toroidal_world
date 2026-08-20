package com.toroidalworld.noise;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;

public final class NoiseRouterBuild {
    private static final ThreadLocal<WorldLoopTransformer> BUILDING = new ThreadLocal<>();

    public static <T> T withTransformer(@Nullable WorldLoopTransformer transformer, Supplier<T> action) {
        WorldLoopTransformer previous = BUILDING.get();
        BUILDING.set(transformer);

        try {
            return action.get();
        } finally {
            BUILDING.set(previous);
        }
    }

    public static @Nullable WorldLoopTransformer wrappedTransformer() {
        return BUILDING.get();
    }

    private NoiseRouterBuild() {
    }
}
