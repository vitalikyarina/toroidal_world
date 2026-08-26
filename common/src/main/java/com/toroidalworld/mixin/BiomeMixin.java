package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.NoiseConstants;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;

@Mixin(Biome.class)
public class BiomeMixin {
    @Shadow
    @Final
    private static PerlinSimplexNoise FROZEN_TEMPERATURE_NOISE;

    @WrapMethod(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z")
    private boolean toroidal$freezeThroughSeam(
            LevelReader level, BlockPos pos, boolean checkNeighbors, Operation<Boolean> original) {
        return toroidal$boundToLevel(level, () -> original.call(level, pos, checkNeighbors));
    }

    @WrapMethod(method = "shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z")
    private boolean toroidal$snowThroughSeam(LevelReader level, BlockPos pos, Operation<Boolean> original) {
        return toroidal$boundToLevel(level, () -> original.call(level, pos));
    }

    @Unique
    private <T> T toroidal$boundToLevel(LevelReader level, Supplier<T> action) {
        WorldFold transformer = WorldLoopAttachments.noiseTransformerOf(level);
        if (transformer == null) {
            return action.get();
        }

        return GenerationTransformerContext.withTransformer(transformer, action);
    }

    @WrapOperation(
            method = "getHeightAdjustedTemperature",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/biome/Biome$TemperatureModifier;modifyTemperature(Lnet/minecraft/core/BlockPos;F)F"))
    private float toroidal$periodicTemperatureModifier(
            Biome.TemperatureModifier modifier, BlockPos pos, float baseTemperature, Operation<Float> original) {
        if (modifier != Biome.TemperatureModifier.FROZEN) {
            return original.call(modifier, pos, baseTemperature);
        }

        Context generation = GenerationTransformerContext.context();
        if (generation.wrappedTransformer() == null) {
            return original.call(modifier, pos, baseTemperature);
        }

        return toroidal$frozenPatchTemperature(generation, pos, baseTemperature);
    }

    @SuppressWarnings("removal")
    @Unique
    private float toroidal$frozenPatchTemperature(Context generation, BlockPos pos, float baseTemperature) {
        double groundValueLargeVariation;
        try (Context.ScaleScope _ = generation.withScale(NoiseConstants.FROZEN_TEMPERATURE_SCALE)) {
            groundValueLargeVariation = FROZEN_TEMPERATURE_NOISE.getValue(pos.getX(), pos.getZ(), false) * 7.0;
        }

        double groundValueEdgeVariation;
        try (Context.ScaleScope _ = generation.withScale(NoiseConstants.BIOME_INFO_EDGE_SCALE)) {
            groundValueEdgeVariation = Biome.BIOME_INFO_NOISE.getValue(pos.getX(), pos.getZ(), false);
        }

        if (groundValueLargeVariation + groundValueEdgeVariation < 0.3) {
            double groundValueSmallVariation;
            try (Context.ScaleScope _ = generation.withScale(NoiseConstants.BIOME_INFO_PATCH_SCALE)) {
                groundValueSmallVariation = Biome.BIOME_INFO_NOISE.getValue(pos.getX(), pos.getZ(), false);
            }

            if (groundValueSmallVariation < 0.8) {
                return 0.2F;
            }
        }

        return baseTemperature;
    }

    @WrapOperation(
            method = "getHeightAdjustedTemperature",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/synth/PerlinSimplexNoise;getValue(DDZ)D"))
    private double toroidal$rawCoordinateHeightNoise(
            PerlinSimplexNoise noise,
            double x,
            double z,
            boolean useNoiseStart,
            Operation<Double> original,
            @Local(argsOnly = true) BlockPos pos) {
        Context generation = GenerationTransformerContext.context();
        if (generation.wrappedTransformer() == null) {
            return original.call(noise, x, z, useNoiseStart);
        }

        try (Context.ScaleScope _ = generation.withScale(NoiseConstants.HEIGHT_TEMPERATURE_SCALE)) {
            return original.call(noise, (double) pos.getX(), (double) pos.getZ(), useNoiseStart);
        }
    }
}
