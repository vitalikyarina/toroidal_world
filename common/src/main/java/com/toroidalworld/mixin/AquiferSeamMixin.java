package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.noise.NoiseConstants;
import com.toroidalworld.noise.QuantizedCoordinates;
import com.toroidalworld.noise.TilingCellGrid;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

@Mixin(targets = "net.minecraft.world.level.levelgen.Aquifer$NoiseBasedAquifer")
public class AquiferSeamMixin {
    @Unique
    private @Nullable TilingCellGrid toroidal$typeGrid;

    @WrapOperation(
            method = "computeSubstance",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;at(III)Lnet/minecraft/util/RandomSource;"))
    private RandomSource toroidal$seedAquiferCellFromCanonical(
            PositionalRandomFactory factory, int gridX, int gridY, int gridZ, Operation<RandomSource> original) {
        WorldLoopTransformer transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(factory, gridX, gridY, gridZ);
        }

        return original.call(factory, transformer.chunks.x.wrap(gridX), gridY, transformer.chunks.z.wrap(gridZ));
    }

    @WrapOperation(
            method = "computeFluidType",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/DensityFunction;compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D"))
    private double toroidal$fluidTypeFromCanonicalCell(
            DensityFunction noise, DensityFunction.FunctionContext cell, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) int blockX,
            @Local(argsOnly = true, ordinal = 2) int blockZ) {
        Context generation = GenerationTransformerContext.context();
        WorldLoopTransformer transformer = generation.wrappedTransformer();
        if (transformer == null) {
            return original.call(noise, cell);
        }

        int vanillaCellWidth = NoiseConstants.AQUIFER_FLUID_TYPE_CELL_WIDTH;
        TilingCellGrid grid = TilingCellGrid.resolve(this.toroidal$typeGrid, transformer, vanillaCellWidth);
        this.toroidal$typeGrid = grid;

        try (Context.DivisorScope divisorScope = generation.withDivisors(grid.xCellWidth(), grid.zCellWidth())) {
            return original.call(noise, QuantizedCoordinates.inBlocks(grid, blockX, cell.blockY(), blockZ));
        }
    }
}
