package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
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

// Where the ice on a frozen ocean is actually decided. Every temperature question — freeze, snow, rain or snow from
// the sky, how far an iceberg melts — comes through getHeightAdjustedTemperature, and it asks two noises that are
// sampled at coordinates vanilla has already scaled. A scaled coordinate is no longer a block position, so the
// periodic sampler cannot fold it; the coordinate is therefore handed over raw and the scale travels in the context,
// exactly as SurfaceSystemMixin does for the iceberg pillars.
//
// The frozen-ocean modifier is reached through the call rather than through its own enum body: an enum constant with a
// body compiles to a class named by its ordinal, which moves whenever vanilla adds a constant before it. The constant
// itself is matched by identity here, which cannot move.
@Mixin(Biome.class)
public class BiomeMixin {
    // Only the private one is shadowed; BIOME_INFO_NOISE is public in vanilla and is referenced directly, so no shadow
    // has to restate a visibility it could get wrong.
    @Shadow
    @Final
    private static PerlinSimplexNoise FROZEN_TEMPERATURE_NOISE;

    // The two methods that actually place ice and snow, and the only point on that path that is handed the level. Every
    // caller arrives here — vanilla's own four (the precipitation tick, the snow-and-freeze feature twice, the lake
    // feature) and anything a mod writes — so binding here is complete in a way binding at the call sites never is:
    // the ordinary way to ask this question is to call the method, not to reach past it.
    //
    // The two-argument shouldFreeze delegates to this one, so it is covered without a wrap of its own.
    @WrapMethod(method = "shouldFreeze(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Z")
    private boolean toroidal$freezeThroughSeam(
            LevelReader level, BlockPos pos, boolean checkNeighbors, Operation<Boolean> original) {
        return toroidal$boundToLevel(level, () -> original.call(level, pos, checkNeighbors));
    }

    @WrapMethod(method = "shouldSnow(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)Z")
    private boolean toroidal$snowThroughSeam(LevelReader level, BlockPos pos, Operation<Boolean> original) {
        return toroidal$boundToLevel(level, () -> original.call(level, pos));
    }

    // A reader that names no level leaves the binding untouched rather than replacing it with NOOP: on the generation
    // side these run inside a chunk step that has already bound the right transformer, and overwriting it would be the
    // one way this fix could make things worse than the call-site bindings it replaces.
    @Unique
    private <T> T toroidal$boundToLevel(LevelReader level, Supplier<T> action) {
        WorldLoopTransformer transformer = WorldLoopAttachments.noiseTransformerOf(level);
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

    // Vanilla-body re-implementation of TemperatureModifier.FROZEN — verified against 26.2; re-diff on a platform bump.
    // The only change is which coordinates the two noises are asked at.
    @SuppressWarnings("removal")
    @Unique
    private float toroidal$frozenPatchTemperature(Context generation, BlockPos pos, float baseTemperature) {
        double groundValueLargeVariation;
        try (Context.ScaleScope scope = generation.withScale(NoiseConstants.FROZEN_TEMPERATURE_SCALE)) {
            groundValueLargeVariation = FROZEN_TEMPERATURE_NOISE.getValue(pos.getX(), pos.getZ(), false) * 7.0;
        }

        double groundValueEdgeVariation;
        try (Context.ScaleScope scope = generation.withScale(NoiseConstants.BIOME_INFO_EDGE_SCALE)) {
            groundValueEdgeVariation = Biome.BIOME_INFO_NOISE.getValue(pos.getX(), pos.getZ(), false);
        }

        if (groundValueLargeVariation + groundValueEdgeVariation < 0.3) {
            double groundValueSmallVariation;
            try (Context.ScaleScope scope = generation.withScale(NoiseConstants.BIOME_INFO_PATCH_SCALE)) {
                groundValueSmallVariation = Biome.BIOME_INFO_NOISE.getValue(pos.getX(), pos.getZ(), false);
            }

            if (groundValueSmallVariation < 0.8) {
                return 0.2F;
            }
        }

        return baseTemperature;
    }

    // The snow line above sea level, which vanilla samples at an eighth of a block. This one sits in Biome's own method
    // body, so the raw position is right there as an argument.
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

        try (Context.ScaleScope scope = generation.withScale(NoiseConstants.HEIGHT_TEMPERATURE_SCALE)) {
            return original.call(noise, (double) pos.getX(), (double) pos.getZ(), useNoiseStart);
        }
    }
}
