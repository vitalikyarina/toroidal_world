package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.engine.noise.ContextScaledNoise;
import com.toroidalworld.engine.noise.DomainWarp;
import com.toroidalworld.engine.noise.DomainWarp.Divisor;
import com.toroidalworld.engine.noise.GenerationTransformerContext;
import com.toroidalworld.engine.noise.GenerationTransformerContext.Context;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$ShiftedNoise")
public class DensityFunctionsShiftedNoiseMixin {
    @Unique
    private @Nullable Divisor toroidal$warpDivisor;

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
            double divisor = this.toroidal$warpDivisor(transformer);
            x = DomainWarp.apply(transformer.blockDomain(Direction.Axis.X), context.blockX(),
                    this.shiftX.compute(context), divisor);
            z = DomainWarp.apply(transformer.blockDomain(Direction.Axis.Z), context.blockZ(),
                    this.shiftZ.compute(context), divisor);
        }

        return ContextScaledNoise.sample(generation, this.noise, x, y, z, this.xzScale,
                GenerationTransformerContext.verticalShare(this.xzScale, this.yScale));
    }

    @Unique
    private double toroidal$warpDivisor(WorldFold transformer) {
        Divisor divisor = this.toroidal$warpDivisor;
        if (divisor == null || divisor.fold() != transformer) {
            divisor = new Divisor(transformer, DomainWarp.divisor(this.noise, transformer, this.xzScale,
                    GenerationTransformerContext.verticalShare(this.xzScale, this.yScale)));
            this.toroidal$warpDivisor = divisor;
        }

        return divisor.value();
    }
}
