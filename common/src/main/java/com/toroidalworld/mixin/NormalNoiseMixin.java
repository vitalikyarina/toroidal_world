package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.accessors.ClimateFieldMark;
import com.toroidalworld.accessors.CoastLiftCache;
import com.toroidalworld.engine.noise.GenerationTransformerContext;
import com.toroidalworld.engine.noise.GenerationTransformerContext.Context;
import com.toroidalworld.engine.noise.NoiseConstants;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.synth.NormalNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

@Mixin(NormalNoise.class)
public class NormalNoiseMixin implements ClimateFieldMark, CoastLiftCache {
    @Shadow
    @Final
    private PerlinNoise first;

    @Shadow
    @Final
    private PerlinNoise second;

    @Shadow
    @Final
    private double valueFactor;

    @Unique
    private volatile double toroidal$coastLift;

    @Override
    public void toroidal$markClimateField() {
        ((ClimateFieldMark) (Object) this.first).toroidal$markClimateField();
        ((ClimateFieldMark) (Object) this.second).toroidal$markClimateField();
    }

    @Override
    public double toroidal$coastLift() {
        return this.toroidal$coastLift;
    }

    @Override
    public void toroidal$coastLift(double lift) {
        this.toroidal$coastLift = lift;
    }

    @WrapMethod(method = "getValue(DDD)D")
    private double toroidal$periodicValue(double x, double y, double z, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(x, y, z);
        }

        return this.toroidal$foldedValue(generation, x, y, z) + this.toroidal$coastLift;
    }

    @Unique
    private double toroidal$foldedValue(Context generation, double x, double y, double z) {
        double firstValue = this.first.getValue(x, y, z);
        double detunedScale = generation.horizontalScale() * NoiseConstants.SECOND_LAYER_DETUNE;
        try (Context.ScaleScope scope = generation.withScale(detunedScale)) {
            double detunedY = generation.slotAxes().y().carriesWorldAxis()
                    ? y
                    : y * NoiseConstants.SECOND_LAYER_DETUNE;
            return (firstValue + this.second.getValue(x, detunedY, z)) * this.valueFactor;
        }
    }
}
