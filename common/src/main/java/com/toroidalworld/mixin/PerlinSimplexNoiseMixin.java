package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.PeriodicSimplexSampler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

// The octave walk, rewritten around the periodic sampler — the simplex twin of PerlinNoiseMixin. X and Z arrive raw,
// because the sampler folds them into the world before it scales them, and the caller's scale rides the context; the
// octave factor multiplies it here, where it becomes that octave's lattice period.
//
// SimplexNoise itself is deliberately left alone. Its other vanilla caller is the End island grid, which already folds
// the integer cell it looks up (DensityFunctionsEndIslandMixin) and would be folded twice by a mixin one level down.
@Mixin(PerlinSimplexNoise.class)
public class PerlinSimplexNoiseMixin {
    @Shadow
    @Final
    private SimplexNoise[] noiseLevels;

    @Shadow
    @Final
    private double highestFreqValueFactor;

    @Shadow
    @Final
    private double highestFreqInputFactor;

    @WrapMethod(method = "getValue(DDZ)D")
    private double toroidal$periodicValue(double x, double z, boolean useNoiseStart, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        WorldLoopTransformer transformer = generation.wrappedTransformer();
        if (transformer == null) {
            return original.call(x, z, useNoiseStart);
        }

        double baseScale = generation.horizontalScale();
        double value = 0.0;
        double factor = this.highestFreqInputFactor;
        double valueFactor = this.highestFreqValueFactor;

        for (SimplexNoise noiseLevel : this.noiseLevels) {
            if (noiseLevel != null) {
                value += PeriodicSimplexSampler.sample(
                        ((SimplexNoiseAccessor) (Object) noiseLevel).toroidal$permutations(),
                        useNoiseStart ? noiseLevel.xo : 0.0,
                        useNoiseStart ? noiseLevel.yo : 0.0,
                        transformer,
                        baseScale * factor,
                        x,
                        z) * valueFactor;
            }

            factor /= 2.0;
            valueFactor *= 2.0;
        }

        return value;
    }
}
