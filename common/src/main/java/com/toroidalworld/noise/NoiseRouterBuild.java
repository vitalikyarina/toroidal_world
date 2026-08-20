package com.toroidalworld.noise;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;

// The transformer the noise router being compiled belongs to, or null for a router that wraps nothing. C2ME builds a
// router synchronously inside RandomState's constructor, so the thread that opens the scope is the one that walks
// every density function of that router.
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
