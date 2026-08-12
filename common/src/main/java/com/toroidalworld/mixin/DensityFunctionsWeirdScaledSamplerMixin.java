package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.levelgen.DensityFunction;

// The one router noise that is sampled at coordinates divided by a position-dependent rarity — spaghetti and noodle
// caves and the cave entrances all come through here on 1.21.1. A divided coordinate is no longer a block position,
// so the periodic fold underneath never engages on the world width and the cave field reads the seam as an edge: a
// cave mouth opens on one side of it and the other side stands as a sheer wall. The rarity is quantized (both mappers
// step through a handful of values), so within a rarity region the scale is constant and travels through the context
// exactly like ShiftedNoise's — the noise is handed the raw block coordinate and folds it on the world circle itself.
//
// The vertical coordinate keeps vanilla's division: Y does not wrap, only the horizontal fold needs the raw value.
//
// 26.2 retired WeirdScaledSampler from the router (QuantizedSpaghettiRarity.wrapRarity2d took its place), so main
// never needs this mixin — it exists only on the version branches whose vanilla still routes caves through it.
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

        try (Context.ScaleScope scope = generation.withScale(1.0 / rarity)) {
            return original.call(noiseHolder, (double) context.blockX(), scaledY, (double) context.blockZ());
        }
    }
}
