package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.BlendedNoise;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

@Mixin(BlendedNoise.class)
public class BlendedNoiseMixin {
    @Shadow
    @Final
    private PerlinNoise minLimitNoise;

    @Shadow
    @Final
    private PerlinNoise maxLimitNoise;

    @Shadow
    @Final
    private PerlinNoise mainNoise;

    @Shadow
    @Final
    private double xzMultiplier;

    @Shadow
    @Final
    private double yMultiplier;

    @Shadow
    @Final
    private double xzFactor;

    @Shadow
    @Final
    private double yFactor;

    @Shadow
    @Final
    private double smearScaleMultiplier;

    @SuppressWarnings("deprecation")
    @WrapMethod(method = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D")
    private double toroidal$periodicCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(context);
        }

        double blockX = context.blockX();
        double blockZ = context.blockZ();
        double limitY = context.blockY() * this.yMultiplier;
        double mainY = limitY / this.yFactor;
        double mainScale = this.xzMultiplier / this.xzFactor;
        double limitSmear = this.yMultiplier * this.smearScaleMultiplier;
        double mainSmear = limitSmear / this.yFactor;
        double blendMin = 0.0;
        double blendMax = 0.0;
        double mainNoiseValue = 0.0;
        double pow = 1.0;

        try (Context.ScaleScope scope = generation.openScale()) {
            for (int i = 0; i < 8; i++) {
                ImprovedNoise noise = this.mainNoise.getOctaveNoise(i);
                if (noise != null) {
                    scope.rescale(mainScale * pow);
                    mainNoiseValue += noise.noise(blockX, PerlinNoise.wrap(mainY * pow), blockZ, mainSmear * pow, mainY * pow) / pow;
                }

                pow /= 2.0;
            }

            double factor = (mainNoiseValue / 10.0 + 1.0) / 2.0;
            boolean isMax = factor >= 1.0;
            boolean isMin = factor <= 0.0;
            pow = 1.0;

            for (int i = 0; i < 16; i++) {
                double wy = PerlinNoise.wrap(limitY * pow);
                double yScalePow = limitSmear * pow;
                scope.rescale(this.xzMultiplier * pow);
                if (!isMax) {
                    ImprovedNoise minNoise = this.minLimitNoise.getOctaveNoise(i);
                    if (minNoise != null) {
                        blendMin += minNoise.noise(blockX, wy, blockZ, yScalePow, limitY * pow) / pow;
                    }
                }

                if (!isMin) {
                    ImprovedNoise maxNoise = this.maxLimitNoise.getOctaveNoise(i);
                    if (maxNoise != null) {
                        blendMax += maxNoise.noise(blockX, wy, blockZ, yScalePow, limitY * pow) / pow;
                    }
                }

                pow /= 2.0;
            }

            return Mth.clampedLerp(factor, blendMin / 512.0, blendMax / 512.0) / 128.0;
        }
    }
}
