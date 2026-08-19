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
