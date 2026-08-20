package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.NoiseConstants;
import com.toroidalworld.noise.QuantizedCoordinates;
import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

@Mixin(targets = "net.minecraft.world.level.levelgen.Aquifer$NoiseBasedAquifer")
public class AquiferSeamMixin {
    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/ishland/c2me/opts/worldgen/general/common/random_instances/RandomUtils;derive(Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;Lnet/minecraft/util/RandomSource;III)V"))
    @TargetHandler(
            mixin = "com.ishland.c2me.opts.worldgen.vanilla.mixin.aquifer.MixinAquiferSamplerImpl",
            name = "onInit")
    private void toroidal$seedAquiferCellFromCanonical(
            PositionalRandomFactory factory,
            RandomSource random,
            int gridX,
            int gridY,
            int gridZ,
            Operation<Void> original) {
        WorldLoopTransformer transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            original.call(factory, random, gridX, gridY, gridZ);
            return;
        }

        original.call(factory, random, transformer.chunks.x.wrap(gridX), gridY, transformer.chunks.z.wrap(gridZ));
    }

    @WrapOperation(
            method = "computeFluidType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/DensityFunction;compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D"))
    private double toroidal$fluidTypeFromCanonicalCell(
            DensityFunction noise, DensityFunction.FunctionContext cell, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(noise, cell);
        }

        int cellWidth = NoiseConstants.AQUIFER_FLUID_TYPE_CELL_WIDTH;

        try (Context.DivisorScope _ = generation.withDivisor(cellWidth)) {
            return original.call(noise, QuantizedCoordinates.inBlocks(cell, cellWidth));
        }
    }
}
