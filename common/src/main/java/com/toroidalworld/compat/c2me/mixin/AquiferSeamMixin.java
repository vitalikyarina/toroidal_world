package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

// The C2ME-shaped twin of com.toroidalworld.mixin.AquiferSeamMixin — the same statement, made where C2ME moved the
// thing it is about.
//
// Vanilla seeds each aquifer cell inside computeSubstance, one positionalRandomFactory.at(gridX, gridY, gridZ) per
// sample on raw grid coordinates, and the mod's own mixin wraps that call. C2ME overwrites computeSubstance and hoists
// the whole grid into a precompute at construction: its onInit walks every cell once and seeds it through
// RandomUtils.derive, which is that same at() spelled out (Mth.getSeed(x, y, z) ^ seedLo). So the raw name a cell
// draws its random under simply moved from the sample to the constructor, and so does the fold: the physical cell
// straddling the seam is asked under two raw names, one per side, and comes out with two fluid levels — water here,
// air there.
//
// Grid Y is untouched, the seam being horizontal, and the block position C2ME caches beside the random stays in each
// side's own frame: only the SEED is canonical, exactly as in the vanilla-shaped fold. The optimisation itself is
// untouched — the precompute still runs once per aquifer, it just names its cells canonically.
//
// Reached through MixinSquared because onInit belongs to C2ME's mixin, not to vanilla; the priority of this config
// clears C2ME's 1100 so the merged body may be injected into at all.
@Mixin(targets = "net.minecraft.world.level.levelgen.Aquifer$NoiseBasedAquifer")
public class AquiferSeamMixin {
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
}
