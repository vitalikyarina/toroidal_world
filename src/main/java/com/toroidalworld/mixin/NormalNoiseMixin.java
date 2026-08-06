package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.NoiseConstants;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

// Vanilla detunes the second layer by a 1.018… factor so the two lattices never resonate. Scaling the coordinate
// would shift the phase of the wrapped sample and leave the second layer unclosed at the seam — so the coordinates
// stay shared and the detune travels through the context scale instead, where it rounds into the second layer's own
// lattice period. At low periods both layers round to the same period and fall back to sharing coordinates; the
// separate seeds still decorrelate them there.
@Mixin(NormalNoise.class)
public class NormalNoiseMixin {
    @Shadow
    @Final
    private PerlinNoise first;

    @Shadow
    @Final
    private PerlinNoise second;

    @Shadow
    @Final
    private double valueFactor;

    @WrapMethod(method = "getValue(DDD)D")
    private double toroidal$periodicValue(double x, double y, double z, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(x, y, z);
        }

        double firstValue = this.first.getValue(x, y, z);
        double detunedScale = generation.horizontalScale() * NoiseConstants.SECOND_LAYER_DETUNE;
        try (Context.ScaleScope _ = generation.withScale(detunedScale)) {
            double detunedY = y * NoiseConstants.SECOND_LAYER_DETUNE;
            return (firstValue + this.second.getValue(x, detunedY, z)) * this.valueFactor;
        }
    }
}
