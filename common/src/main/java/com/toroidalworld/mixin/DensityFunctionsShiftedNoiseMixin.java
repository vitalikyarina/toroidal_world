package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.ContextScaledNoise;
import com.toroidalworld.noise.DomainWarp;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$ShiftedNoise")
public class DensityFunctionsShiftedNoiseMixin {
    @Shadow
    @Final
    private DensityFunction shiftX;

    @Shadow
    @Final
    private DensityFunction shiftY;

    @Shadow
    @Final
    private DensityFunction shiftZ;

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
        WorldFold transformer = generation.wrappedTransformer();
        if (transformer == null) {
            return original.call(context);
        }

        double x = context.blockX();
        double y = context.blockY() * this.yScale + this.shiftY.compute(context);
        double z = context.blockZ();
        if (this.xzScale != 0.0) {
            x = DomainWarp.apply(transformer.blockDomain(Direction.Axis.X), context.blockX(),
                    this.shiftX.compute(context), this.xzScale);
            z = DomainWarp.apply(transformer.blockDomain(Direction.Axis.Z), context.blockZ(),
                    this.shiftZ.compute(context), this.xzScale);
        }

        return ContextScaledNoise.sample(generation, this.noise, x, y, z, this.xzScale,
                GenerationTransformerContext.verticalShare(this.xzScale, this.yScale));
    }
}
