package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.PeriodicOctaveSampler;
import com.bawnorton.mixinsquared.TargetHandler;
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

    @WrapMethod(method = "@MixinSquared:Handler")
    @TargetHandler(
            mixin = "com.ishland.c2me.opts.math.mixin.MixinOctavePerlinNoiseSampler",
            name = "getValue")
    private double toroidal$periodicValue(double x, double y, double z, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(x, y, z);
        }

        // The zeros are what C2ME's loop passes every octave, kept so the two walks differ in nothing but the fold.
        return PeriodicOctaveSampler.sample(generation, this.noiseLevels, this.amplitudes,
                this.lowestFreqInputFactor, this.lowestFreqValueFactor, x, y, z, 0.0, 0.0);
    }
}
