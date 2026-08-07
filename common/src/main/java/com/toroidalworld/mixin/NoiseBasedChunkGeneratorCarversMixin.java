package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.gen.ShapedChunkGenerator;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.WorldgenRandom;

// A cave came out cut at the seam. applyCarvers scans a raw ±8 square of source chunks and seeds each carver from the
// source's RAW coordinates. Two chunks on opposite sides of the seam reach the same physical source under different raw
// names, so setLargeFeatureSeed hands them different seeds: one side decides a cave starts there and shapes it one way,
// the other decides differently. The two halves never agree the cave exists, so it stops at the boundary.
//
// The seed is the only seam-blind part. carve writes only into the chunk being generated (an in-bounds proto), through
// the carving mask, so there is no phantom write to fold — and the carve origin stays on the raw source position, which
// is the nearest copy around the centre, so its geometry lands in this chunk's frame. Seeding from the CANONICAL source
// instead makes both sides compute the same physical cave, each carving its own half, and the cave is continuous.
//
// No dedup, unlike the reference scan: a reference is a discrete thing (adding one twice places the structure on itself),
// but a carve is geometry. On a world narrower than the scan the far copy of a source only carves what genuinely reaches
// this chunk the long way around the torus — the wrap-around of a cave longer than the world — which is correct, not a
// duplicate.
@Mixin(NoiseBasedChunkGenerator.class)
public class NoiseBasedChunkGeneratorCarversMixin {
    @WrapOperation(
            method = "applyCarvers",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/levelgen/WorldgenRandom;setLargeFeatureSeed(JII)V"))
    private void toroidal$seedCarverFromCanonicalSource(
            WorldgenRandom random, long seed, int sourceChunkX, int sourceChunkZ, Operation<Void> original) {
        WorldLoopTransformer transformer =
                ShapedChunkGenerator.wrappedTransformerOf((NoiseBasedChunkGenerator) (Object) this);
        if (transformer == null) {
            original.call(random, seed, sourceChunkX, sourceChunkZ);
            return;
        }

        original.call(random, seed,
                transformer.chunks.x.wrap(sourceChunkX), transformer.chunks.z.wrap(sourceChunkZ));
    }
}
