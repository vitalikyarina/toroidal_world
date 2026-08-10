package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.GoalUtils;

// One level above the home radius itself sits the switch that decides whether the radius is consulted at all: a mob far
// enough outside its home has the filter turned off, so that it is not left with every candidate rejected and nowhere
// to walk. That "far enough" is another raw subtraction against the home centre.
//
// Across the seam it reads a world out and turns the filter off for a mob standing a few steps from its own home, which
// is the one case it exists to keep on. The error only ever goes this way — a folded distance is never longer than the
// raw one — so the radius near the boundary holds more loosely than anywhere else in the world, and a homed mob drifts
// past it until the restriction goal drags it back.
//
// The centre becomes its copy nearest the mob and vanilla's own comparison runs on that, as in RandomPosMixin.
@Mixin(GoalUtils.class)
public class GoalUtilsMixin {
    @ModifyExpressionValue(
            method = "mobRestricted",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/PathfinderMob;getRestrictCenter()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos toroidal$homeFilterThroughSeam(BlockPos home, @Local(argsOnly = true) PathfinderMob mob) {
        return SeamSteering.nearestCopy(mob, home);
    }
}
