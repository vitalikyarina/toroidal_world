package com.toroidalworld.noise;

import com.toroidalworld.noise.GenerationTransformerContext.Context;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

// The octave walk of a periodic PerlinNoise, in one place because it now has two callers: vanilla's six-argument
// getValue, which this mod wraps, and the three-argument one C2ME overwrites with an octave loop of its own.
//
// X and Z go to ImprovedNoise raw: the wrapped sampler maps them onto a circle spanning the world, so scaling them by
// the octave factor — or folding them through PerlinNoise.wrap, as vanilla and C2ME both do — would shift the phase
// and tear the seam open. The octave factor travels through the context instead, where it becomes the radius of that
// circle. Y keeps vanilla's treatment exactly: it is scaled and wrapped, having no seam.
//
// useNoiseOrigin is vanilla's own flag on this game version — it cancels each octave's Y offset instead of scaling the
// coordinate, and it has to be carried rather than assumed: C2ME's overwritten loop is the false case, and folding the
// two together would silently move Y for every caller that asks for the origin.
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
