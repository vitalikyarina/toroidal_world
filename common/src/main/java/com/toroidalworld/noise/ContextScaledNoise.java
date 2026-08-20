package com.toroidalworld.noise;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext.Context;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class ContextScaledNoise {
    // For a caller already holding the context it was bound by: fetching it again here is the second thread-local
    // lookup per sample that measured ~9% over a chunk's noise fill.
    public static double sample(Context context, DensityFunction.NoiseHolder noise,
            double x, double y, double z, double horizontalScale) {
        try (Context.ScaleScope scope = context.withScale(horizontalScale)) {
            return noise.getValue(x, y, z);
        }
    }

    // For the compiled router, whose transformer is a constant of the class C2ME generated for it. Binding it rather
    // than reading it means a sample taken on a thread nothing bound still walks the wrapped lattice, instead of
    // quietly producing vanilla terrain that then goes to disk.
    public static double sampleWrapped(WorldLoopTransformer transformer, DensityFunction.NoiseHolder noise,
            double x, double y, double z, double horizontalScale) {
        Context context = GenerationTransformerContext.context();

        try (Context.TransformerScope transformerScope = context.bindTransformer(transformer);
                Context.ScaleScope scaleScope = context.withScale(horizontalScale)) {
            return noise.getValue(x, y, z);
        }
    }

    private ContextScaledNoise() {
    }
}
