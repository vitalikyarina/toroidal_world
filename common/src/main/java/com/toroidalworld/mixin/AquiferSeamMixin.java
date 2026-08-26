package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

@Mixin(targets = "net.minecraft.world.level.levelgen.Aquifer$NoiseBasedAquifer")
public class AquiferSeamMixin {
    @WrapOperation(
            method = "computeSubstance",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;at(III)Lnet/minecraft/util/RandomSource;"))
    private RandomSource toroidal$seedAquiferCellFromCanonical(
            PositionalRandomFactory factory, int gridX, int gridY, int gridZ, Operation<RandomSource> original) {
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(factory, gridX, gridY, gridZ);
        }

        long canonical = transformer.foldChunkKey(ChunkPos.pack(gridX, gridZ));
        return original.call(factory, ChunkPos.getX(canonical), gridY, ChunkPos.getZ(canonical));
    }
}
