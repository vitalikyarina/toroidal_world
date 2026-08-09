package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.stateproviders.DualNoiseProvider;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

@Mixin(DualNoiseProvider.class)
public class DualNoiseProviderMixin {
    @Shadow
    @Final
    private NormalNoise slowNoise;

    @Shadow
    @Final
    private float slowScale;

    @WrapMethod(method = "getSlowNoiseValue(Lnet/minecraft/core/BlockPos;)D")
    private double toroidal$rawCoordinateNoise(BlockPos pos, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(pos);
        }

        try (Context.ScaleScope scope = generation.withScale(this.slowScale)) {
            return this.slowNoise.getValue(pos.getX(), pos.getY() * this.slowScale, pos.getZ());
        }
    }
}
