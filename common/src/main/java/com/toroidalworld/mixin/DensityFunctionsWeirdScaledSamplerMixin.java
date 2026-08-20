package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$WeirdScaledSampler")
public class DensityFunctionsWeirdScaledSamplerMixin {
    @WrapOperation(
            method = "transform",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/DensityFunction$NoiseHolder;getValue(DDD)D"))
    private double toroidal$periodicRaritySample(
            DensityFunction.NoiseHolder noiseHolder,
            double scaledX,
            double scaledY,
            double scaledZ,
            Operation<Double> original,
            @Local(argsOnly = true) DensityFunction.FunctionContext context,
            @Local(ordinal = 1) double rarity) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(noiseHolder, scaledX, scaledY, scaledZ);
        }

        // Y keeps vanilla's division: only the horizontal coordinate needs the raw block value to fold on the world circle.
        try (Context.ScaleScope scope = generation.withScale(1.0 / rarity)) {
            return original.call(noiseHolder, (double) context.blockX(), scaledY, (double) context.blockZ());
        }
    }
}
