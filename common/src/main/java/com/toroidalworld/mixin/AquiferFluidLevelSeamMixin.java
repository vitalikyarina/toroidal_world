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

import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(targets = "net.minecraft.world.level.levelgen.Aquifer$NoiseBasedAquifer")
public class AquiferFluidLevelSeamMixin {
    @Unique
    private @Nullable TilingCellGrid toroidal$levelGrid;

    @WrapOperation(
            method = "computeRandomizedFluidSurfaceLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/DensityFunction;compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D"))
    private double toroidal$fluidLevelFromCanonicalCell(
            DensityFunction noise, DensityFunction.FunctionContext cell, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) int blockX,
            @Local(argsOnly = true, ordinal = 2) int blockZ) {
        Context generation = GenerationTransformerContext.context();
        WorldLoopTransformer transformer = generation.wrappedTransformer();
        if (transformer == null) {
            return original.call(noise, cell);
        }

        int vanillaCellWidth = NoiseConstants.AQUIFER_FLUID_LEVEL_CELL_WIDTH;
        TilingCellGrid grid = TilingCellGrid.resolve(this.toroidal$levelGrid, transformer, vanillaCellWidth);
        this.toroidal$levelGrid = grid;

        try (Context.DivisorScope _ = generation.withDivisors(grid.xCellWidth(), grid.zCellWidth())) {
            return original.call(noise, QuantizedCoordinates.inBlocks(grid, blockX, cell.blockY(), blockZ));
        }
    }
}
