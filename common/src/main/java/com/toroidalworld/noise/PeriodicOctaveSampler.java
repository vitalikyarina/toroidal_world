package com.toroidalworld.noise;

import com.toroidalworld.noise.GenerationTransformerContext.Context;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

public final class PeriodicOctaveSampler {
    @SuppressWarnings("deprecation")
    public static double sample(
            Context generation,
            ImprovedNoise[] noiseLevels,
            DoubleList amplitudes,
            double lowestFreqInputFactor,
            double lowestFreqValueFactor,
            double x,
            double y,
            double z,
            double yScale,
            double yFudge,
            boolean useNoiseOrigin) {
        double baseScale = generation.horizontalScale();
        double value = 0.0;
        double factor = lowestFreqInputFactor;
        double valueFactor = lowestFreqValueFactor;

        try (Context.ScaleScope scope = generation.openScale()) {
            for (int i = 0; i < noiseLevels.length; i++) {
                ImprovedNoise noise = noiseLevels[i];
                if (noise != null) {
                    scope.rescale(baseScale * factor);
                    double noiseValue = noise.noise(x,
                            useNoiseOrigin ? -noise.yo : PerlinNoise.wrap(y * factor),
                            z, yScale * factor, yFudge * factor);
                    value += amplitudes.getDouble(i) * noiseValue * valueFactor;
                }

                factor *= 2.0;
                valueFactor /= 2.0;
            }
        }

        return value;
    }

    private PeriodicOctaveSampler() {
    }
}
