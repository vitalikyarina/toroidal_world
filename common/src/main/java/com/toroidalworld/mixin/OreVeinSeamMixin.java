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

//
// The filler is a lambda, and a lambda body is an ordinary private method once the game is obfuscated — so its name is
// whatever the running loader's mappings call it. Mojmap restores javac's lambda$create$0 for NeoForge; intermediary
// gives it an id of its own, method_40547, for Fabric. Both are listed and defaultRequire = 1 takes whichever exists,
// the same dual-name pattern GuiMapMixin uses for its Screen override.
@Mixin(OreVeinifier.class)
public class OreVeinSeamMixin {
    @WrapOperation(
            method = {"lambda$create$0", "method_40547"},
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
