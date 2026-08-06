package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.stateproviders.NoiseBasedStateProvider;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

// Feature state providers scale X/Z before sampling; the wrapped noise needs the raw block position and takes the
// scale from the context, otherwise the same feature would pick different blocks on the two sides of the seam.
@Mixin(NoiseBasedStateProvider.class)
public class NoiseBasedStateProviderMixin {
    @Shadow
    @Final
    protected NormalNoise noise;

    @WrapMethod(method = "getNoiseValue(Lnet/minecraft/core/BlockPos;D)D")
    private double toroidal$rawCoordinateNoise(BlockPos pos, double scale, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(pos, scale);
        }

        try (Context.ScaleScope _ = generation.withScale(scale)) {
            return this.noise.getValue(pos.getX(), pos.getY() * scale, pos.getZ());
        }
    }
}
