package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.noise.ContextScaledNoise;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$ShiftedNoise")
public class DensityFunctionsShiftedNoiseMixin {
    @Shadow
    @Final
    private DensityFunction shiftY;

    @Shadow
    @Final
    private double xzScale;

    @Shadow
    @Final
    private double yScale;

    @Shadow
    @Final
    private DensityFunction.NoiseHolder noise;

    @WrapMethod(method = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D")
    private double toroidal$periodicCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(context);
        }

        double y = context.blockY() * this.yScale + this.shiftY.compute(context);

        double verticalShare = this.xzScale == 0.0 ? GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE
                : this.yScale / this.xzScale;

        return ContextScaledNoise.sample(generation, this.noise,
                context.blockX(), y, context.blockZ(), this.xzScale,
                verticalShare);
    }
}
