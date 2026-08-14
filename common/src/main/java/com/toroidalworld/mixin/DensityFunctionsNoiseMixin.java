package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.DensityFunction;

// The horizontal coordinate reaches ImprovedNoise raw, where it is mapped onto the world circle; xzScale travels
// through the context instead and sizes that circle.
@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$Noise")
public class DensityFunctionsNoiseMixin {
    @Shadow
    @Final
    private DensityFunction.NoiseHolder noise;

    @Shadow
    @Final
    private double xzScale;

    @Shadow
    @Final
    private double yScale;

    @WrapMethod(method = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D")
    private double toroidal$periodicCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(context);
        }

        double verticalShare = this.xzScale == 0.0 ? GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE
                : this.yScale / this.xzScale;
        try (Context.ScaleScope _ = generation.withScale(this.xzScale, verticalShare)) {
            return this.noise.getValue(context.blockX(), context.blockY() * this.yScale, context.blockZ());
        }
    }
}
