package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.OreVeinifier;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

@Mixin(OreVeinifier.class)
public class OreVeinSeamMixin {
    @WrapOperation(
            method = "lambda$create$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;at(III)Lnet/minecraft/util/RandomSource;"))
    private static RandomSource toroidal$seedVeinFromCanonical(
            PositionalRandomFactory factory, int blockX, int blockY, int blockZ, Operation<RandomSource> original) {
        WorldLoopTransformer transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(factory, blockX, blockY, blockZ);
        }

        return original.call(factory, transformer.coords.x.wrap(blockX), blockY, transformer.coords.z.wrap(blockZ));
    }
}
