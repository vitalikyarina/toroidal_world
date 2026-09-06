package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.accessors.ClimateCompressionCache;
import com.toroidalworld.accessors.ClimateFieldMark;
import com.toroidalworld.noise.ClimateScaleCompression.Resolved;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.PeriodicOctaveSampler;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

@Mixin(PerlinNoise.class)
public class PerlinNoiseMixin implements ClimateCompressionCache, ClimateFieldMark {
    @Unique
    private @Nullable Resolved toroidal$climateCompression;

    @Unique
    private volatile boolean toroidal$climateField;

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

    @WrapMethod(method = "getValue(DDDDD)D")
    private double toroidal$periodicValue(double x, double y, double z, double yScale, double yFudge, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(x, y, z, yScale, yFudge);
        }

        return PeriodicOctaveSampler.sample(generation, this, this.toroidal$climateField, this.noiseLevels,
                this.amplitudes, this.lowestFreqInputFactor, this.lowestFreqValueFactor, x, y, z, yScale, yFudge);
    }

    @Override
    public boolean toroidal$climateField() {
        return this.toroidal$climateField;
    }

    @Override
    public void toroidal$markClimateField() {
        this.toroidal$climateField = true;
    }

    @Override
    public @Nullable Resolved toroidal$climateCompression() {
        return this.toroidal$climateCompression;
    }

    @Override
    public void toroidal$climateCompression(Resolved resolved) {
        this.toroidal$climateCompression = resolved;
    }
}
