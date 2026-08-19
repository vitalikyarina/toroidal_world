package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.PeriodicOctaveSampler;
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

    @WrapMethod(method = "getValue(DDDDDZ)D")
    private double toroidal$periodicValue(double x, double y, double z, double yScale, double yFudge,
            boolean useNoiseOrigin, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(x, y, z, yScale, yFudge, useNoiseOrigin);
        }

        return PeriodicOctaveSampler.sample(generation, this.noiseLevels, this.amplitudes,
                this.lowestFreqInputFactor, this.lowestFreqValueFactor, x, y, z, yScale, yFudge, useNoiseOrigin);
    }
}
