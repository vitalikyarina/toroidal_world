package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;

@Mixin(NoiseBasedChunkGenerator.class)
public class NoiseBasedChunkGeneratorCarversMixin {
    @WrapOperation(
            method = "applyCarvers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;setLargeFeatureSeed(JII)V"))
    private void toroidal$seedCarverFromCanonicalSource(
            WorldgenRandom random, long seed, int sourceChunkX, int sourceChunkZ, Operation<Void> original) {
        WorldFold transformer =
                ShapedChunkGenerator.wrappedTransformerOf((NoiseBasedChunkGenerator) (Object) this);
        if (transformer == null) {
            original.call(random, seed, sourceChunkX, sourceChunkZ);
            return;
        }

        long folded = transformer.foldChunkKey(ChunkPos.asLong(sourceChunkX, sourceChunkZ));
        original.call(random, seed,
                ChunkPos.getX(folded), ChunkPos.getZ(folded));
    }
}
