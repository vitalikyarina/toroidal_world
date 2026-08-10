package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

@Mixin(PerlinNoise.class)
public class PerlinNoiseMixin {
    @Shadow
    @Final
    private ImprovedNoise[] noiseLevels;

    @Shadow
    @Final
    private DoubleList amplitudes;

    @Shadow
    @Final
    private double lowestFreqValueFactor;

    @Shadow
    @Final
    private double lowestFreqInputFactor;

    // X and Z go in raw: the wrapped ImprovedNoise maps them onto a circle spanning the world, so scaling them by the
    // octave factor (or folding them through PerlinNoise.wrap) would shift the phase and tear the seam open. The octave
    // factor travels through the context instead, where it becomes the radius of that circle.
    @SuppressWarnings("deprecation")
    @WrapMethod(method = "getValue(DDDDDZ)D")
    private double toroidal$periodicValue(double x, double y, double z, double yScale, double yFudge,
            boolean useNoiseOrigin, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(x, y, z, yScale, yFudge, useNoiseOrigin);
        }

        double baseScale = generation.horizontalScale();
        double value = 0.0;
        double factor = this.lowestFreqInputFactor;
        double valueFactor = this.lowestFreqValueFactor;

        try (Context.ScaleScope scope = generation.openScale()) {
            for (int i = 0; i < this.noiseLevels.length; i++) {
                ImprovedNoise noise = this.noiseLevels[i];
                if (noise != null) {
                    scope.rescale(baseScale * factor);
                    double noiseValue = noise.noise(x,
                            useNoiseOrigin ? -noise.yo : PerlinNoise.wrap(y * factor),
                            z, yScale * factor, yFudge * factor);
                    value += this.amplitudes.getDouble(i) * noiseValue * valueFactor;
                }

                factor *= 2.0;
                valueFactor /= 2.0;
            }
        }

        return value;
    }
}
