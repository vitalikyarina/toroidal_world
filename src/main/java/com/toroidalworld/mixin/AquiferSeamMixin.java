package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

// The carver fold makes the cave cavity continuous across the seam, but the water filling it was still cut: a carved
// space came out dry on one side of the boundary and flooded on the other. The fill is the aquifer's, and its per-cell
// fluid level is seeded by positionalRandomFactory.at(gridX, gridY, gridZ) on RAW grid coordinates — grid X/Z are just
// the chunk coordinate (blockCoord >> 4). The same physical aquifer cell straddling the seam is asked under two raw
// names, one from each side, so it draws two different randoms → two different fluid levels → water here, air there.
//
// The fluid-level density functions themselves are periodic noise and already wrap; only this positional random is
// blind. Seeding it from the CANONICAL cell (the chunk coordinate wrapped into the world) makes both sides pick the same
// level for the one physical cell. Grid Y is untouched — the seam is horizontal. The block location the random then
// picks stays in each side's own frame, physically the same point, and computeFluid reads it through the wrapped noise,
// so nothing else has to move.
//
// The transformer is read from the thread-bound generation context, the same channel the periodic noise samplers use;
// a plain generator leaves it NOOP and the seed stays raw, byte-for-byte vanilla.
@Mixin(targets = "net.minecraft.world.level.levelgen.Aquifer$NoiseBasedAquifer")
public class AquiferSeamMixin {
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
}
