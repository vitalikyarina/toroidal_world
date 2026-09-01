package com.toroidalworld.noise;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.GenerationTransformerContext.Context;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;
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
        double declaredScale = generation.horizontalScale();
        WorldFold transformer = generation.transformer();
        double baseScale = declaredScale * ClimateScaleCompression.factor(
                transformer.blockDomain(Direction.Axis.X), transformer.blockDomain(Direction.Axis.Z),
                amplitudes, lowestFreqInputFactor, declaredScale, generation.verticalShare());
        boolean yCarriesWorldAxis = generation.slotAxes().y().carriesWorldAxis();
        double value = 0.0;
        double factor = lowestFreqInputFactor;
        double valueFactor = lowestFreqValueFactor;

        try (Context.ScaleScope scope = generation.openScale()) {
            for (int i = 0; i < noiseLevels.length; i++) {
                ImprovedNoise noise = noiseLevels[i];
                if (noise != null) {
                    scope.rescale(baseScale * factor);
                    double slotY = useNoiseOrigin
                            ? -noise.yo
                            : yCarriesWorldAxis ? y : PerlinNoise.wrap(y * factor);
                    double noiseValue = noise.noise(x, slotY, z, yScale * factor, yFudge * factor);
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
