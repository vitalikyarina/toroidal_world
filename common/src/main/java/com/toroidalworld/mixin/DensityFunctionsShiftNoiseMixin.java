package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.engine.noise.ContextScaledNoise;
import com.toroidalworld.engine.noise.GenerationTransformerContext;
import com.toroidalworld.engine.noise.GenerationTransformerContext.Context;
import com.toroidalworld.engine.noise.NoiseConstants;
import com.toroidalworld.engine.noise.SlotAxes;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$ShiftNoise")
public interface DensityFunctionsShiftNoiseMixin {
    @Shadow
    DensityFunction.NoiseHolder offsetNoise();

    @WrapMethod(method = "compute(DDD)D")
    default double toroidal$periodicCompute(double localX, double localY, double localZ, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(localX, localY, localZ);
        }

        SlotAxes axes = generation.slotAxes();

        double sample = ContextScaledNoise.sample(generation, this.offsetNoise(),
                axes.x().samplerInput(localX, NoiseConstants.SHIFT_SCALE),
                axes.y().samplerInput(localY, NoiseConstants.SHIFT_SCALE),
                axes.z().samplerInput(localZ, NoiseConstants.SHIFT_SCALE),
                NoiseConstants.SHIFT_SCALE);

        return sample * NoiseConstants.SHIFT_AMPLITUDE;
    }
}
