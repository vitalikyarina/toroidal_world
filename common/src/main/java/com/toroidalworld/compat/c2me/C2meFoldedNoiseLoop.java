package com.toroidalworld.compat.c2me;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.SlotAxes;

import net.minecraft.world.level.levelgen.DensityFunction;

public final class C2meFoldedNoiseLoop {
    // A null axis array means C2ME const-eliminated that input; its value is the paired constant.
    public static void fill(WorldLoopTransformer transformer, SlotAxes axes, DensityFunction.NoiseHolder noise,
            double[] res, double @Nullable [] xs, double xConst, double @Nullable [] ys, double yConst,
            double @Nullable [] zs, double zConst, double horizontalScale) {
        Context context = GenerationTransformerContext.context();

        try (Context.BindingScope bindingScope = context.bind(transformer, axes, horizontalScale)) {
            for (int i = 0; i < res.length; i++) {
                double x = xs != null ? xs[i] : xConst;
                double y = ys != null ? ys[i] : yConst;
                double z = zs != null ? zs[i] : zConst;
                res[i] = noise.getValue(x, y, z);
            }
        }
    }

    private C2meFoldedNoiseLoop() {
    }
}
