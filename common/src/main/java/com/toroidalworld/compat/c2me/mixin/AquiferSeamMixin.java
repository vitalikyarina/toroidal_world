package com.toroidalworld.compat.c2me.mixin;

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
import com.bawnorton.mixinsquared.TargetHandler;
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
