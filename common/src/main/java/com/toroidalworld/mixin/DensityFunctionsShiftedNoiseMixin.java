package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.DensityFunction;

// The horizontal shifts warp the sampling domain, which breaks the phase of the wrapped noise — only the vertical
// shift survives, X/Z reach the noise raw, and xzScale travels through the context.
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
        try (Context.ScaleScope _ = generation.withScale(this.xzScale, verticalShare)) {
            return this.noise.getValue(context.blockX(), y, context.blockZ());
        }
    }
}
