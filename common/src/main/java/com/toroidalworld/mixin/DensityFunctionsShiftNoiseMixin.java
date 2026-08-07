package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.NoiseConstants;

import net.minecraft.world.level.levelgen.DensityFunction;

// The 0.25 scale stays applied to Y directly; horizontally it travels through the context, because scaling X/Z would
// shift the phase of the wrapped noise.
@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$ShiftNoise")
public interface DensityFunctionsShiftNoiseMixin {
    @Shadow
    DensityFunction.NoiseHolder offsetNoise();

    @Inject(method = "compute(DDD)D", at = @At("HEAD"), cancellable = true)
    default void toroidal$periodicCompute(double localX, double localY, double localZ, CallbackInfoReturnable<Double> cir) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return;
        }

        try (Context.ScaleScope _ = generation.withScale(NoiseConstants.SHIFT_SCALE)) {
            cir.setReturnValue(this.offsetNoise().getValue(localX, localY * NoiseConstants.SHIFT_SCALE, localZ) * 4.0);
        }
    }
}
