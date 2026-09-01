package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.noise.CanonicalCellSampler;
import com.toroidalworld.noise.NoiseConstants;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.level.levelgen.DensityFunction;

@Mixin(targets = "net.minecraft.world.level.levelgen.Aquifer$NoiseBasedAquifer")
public class AquiferFluidLevelSeamMixin {
    @Unique
    private @Nullable CanonicalCellSampler toroidal$levelSampler;

    @WrapOperation(
            method = "computeRandomizedFluidSurfaceLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/DensityFunction;compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D"))
    private double toroidal$fluidLevelFromCanonicalCell(
            DensityFunction noise, DensityFunction.FunctionContext cell, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) int blockX,
            @Local(argsOnly = true, ordinal = 2) int blockZ) {
        if (this.toroidal$levelSampler == null) {
            this.toroidal$levelSampler = new CanonicalCellSampler(NoiseConstants.AQUIFER_FLUID_LEVEL_CELL_WIDTH);
        }

        return this.toroidal$levelSampler.sample(cell, blockX, blockZ, folded -> original.call(noise, folded));
    }
}
