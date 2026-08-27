package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;

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
        WorldFold transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            original.call(factory, random, gridX, gridY, gridZ);
            return;
        }

        long canonical = transformer.foldChunkKey(ChunkPos.asLong(gridX, gridZ));
        original.call(factory, random, ChunkPos.getX(canonical), gridY, ChunkPos.getZ(canonical));
    }
}
