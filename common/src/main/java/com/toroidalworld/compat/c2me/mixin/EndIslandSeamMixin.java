package com.toroidalworld.compat.c2me.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.PeriodicEndIslands;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

// The C2ME-shaped twin of com.toroidalworld.mixin.DensityFunctionsEndIslandMixin, and the reason that one goes quiet:
// C2ME's opts/natives_math overwrites compute with a native sample keyed on the raw block coordinate, from a config of
// priority 1100 against the mod's default 1000 — so the vanilla-shaped wrapper is merged first and then replaced
// wholesale, with no error and no warning.
//
// The same statement, then, made from this config's 1200, which lands after C2ME's overwrite rather than under it. No
// MixinSquared here: an overwrite leaves no handler to target, it leaves the method itself carrying someone else's
// body, and a wrapper does not care whose body it wraps.
//
// An unwrapped level falls through to C2ME's native walk, which is left exactly as it is.
@Mixin(targets = "net.minecraft.world.level.levelgen.DensityFunctions$EndIslandDensityFunction")
public class EndIslandSeamMixin {
    @Shadow
    @Final
    private SimplexNoise islandNoise;

    @WrapMethod(method = "compute(Lnet/minecraft/world/level/levelgen/DensityFunction$FunctionContext;)D")
    private double toroidal$loopedCompute(DensityFunction.FunctionContext context, Operation<Double> original) {
        WorldLoopTransformer transformer = GenerationTransformerContext.context().wrappedTransformer();
        if (transformer == null) {
            return original.call(context);
        }

        return PeriodicEndIslands.density(
                PeriodicEndIslands.heightValue(this.islandNoise, transformer, context.blockX(), context.blockZ()));
    }
}
