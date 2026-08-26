package com.toroidalworld.noise;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.GenerationTransformerContext.Context;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class ContextScaledNoise {
    public static double sample(Context context, DensityFunction.NoiseHolder noise,
            double x, double y, double z, double horizontalScale) {
        try (Context.ScaleScope scope = context.withScale(horizontalScale)) {
            return noise.getValue(x, y, z);
        }
    }

    // Bound rather than read: a sample on a thread nothing bound would otherwise write vanilla terrain to disk.
    public static double sampleWrapped(WorldFold transformer, SlotAxes axes,
            DensityFunction.NoiseHolder noise,
            double x, double y, double z, double horizontalScale) {
        Context context = GenerationTransformerContext.context();

        try (Context.BindingScope bindingScope = context.bind(transformer, axes, horizontalScale)) {
            return noise.getValue(x, y, z);
        }
    }

    private ContextScaledNoise() {
    }
}
