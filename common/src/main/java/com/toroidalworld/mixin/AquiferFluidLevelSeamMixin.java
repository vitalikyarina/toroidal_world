package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.NoiseConstants;
import com.toroidalworld.noise.QuantizedCoordinates;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(targets = "net.minecraft.world.level.levelgen.Aquifer$NoiseBasedAquifer")
public class AquiferFluidLevelSeamMixin {
    @WrapOperation(
            method = "computeRandomizedFluidSurfaceLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/DensityFunction;compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D"))
    private double toroidal$fluidLevelFromCanonicalCell(
            DensityFunction noise, DensityFunction.FunctionContext cell, Operation<Double> original) {
        Context generation = GenerationTransformerContext.context();
        if (!generation.transformer().isWrapped()) {
            return original.call(noise, cell);
        }

        int cellWidth = NoiseConstants.AQUIFER_FLUID_LEVEL_CELL_WIDTH;

        try (Context.DivisorScope divisorScope = generation.withDivisor(cellWidth)) {
            return original.call(noise, QuantizedCoordinates.inBlocks(cell, cellWidth));
        }
    }
}
