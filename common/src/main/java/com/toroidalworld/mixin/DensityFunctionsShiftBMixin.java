package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.DensityFunctionSlotAxes;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.NoiseConstants;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$ShiftB")
public class DensityFunctionsShiftBMixin {
    @Shadow
    @Final
    private DensityFunction.NoiseHolder offsetNoise;

    @WrapMethod(method = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D")
    private double toroidal$periodicCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        WorldLoopTransformer transformer = generation.wrappedTransformer();
        if (transformer == null) {
            return original.call(context);
        }

        try (Context.BindingScope _ = generation.bind(transformer, DensityFunctionSlotAxes.SHIFT_B,
                NoiseConstants.SHIFT_SCALE)) {
            return this.offsetNoise.getValue(context.blockZ(), context.blockX(), 0.0)
                    * NoiseConstants.SHIFT_AMPLITUDE;
        }
    }
}
