package com.toroidalworld.compat.c2me;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class C2meDfcFold {
    public static @Nullable Context wrappedContext() {
        Context context = GenerationTransformerContext.context();
        return context.transformer().isWrapped() ? context : null;
    }

    public static double sample(Context context, DensityFunction.NoiseHolder noise,
            double x, double y, double z, double horizontalScale) {
        try (Context.ScaleScope scope = context.withScale(horizontalScale)) {
            return noise.getValue(x, y, z);
        }
    }

    private C2meDfcFold() {
    }
}
