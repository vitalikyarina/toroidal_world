package com.toroidalworld.noise;

import com.toroidalworld.noise.GenerationTransformerContext.Context;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class ContextScaledNoise {
    public static double sample(Context context, DensityFunction.NoiseHolder noise,
            double x, double y, double z, double horizontalScale) {
        try (Context.ScaleScope _ = context.withScale(horizontalScale)) {
            return noise.getValue(x, y, z);
        }
    }

    private ContextScaledNoise() {
    }
}
