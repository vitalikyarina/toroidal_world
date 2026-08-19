package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.RandomPos;

// The priority is raised because a mod may replace this method wholesale — Sable does, for its sublevels — and mixin
// refuses an injection into a method merged by another mixin of equal priority. The read this fold modifies survives
// such a replacement; the arithmetic around it is the other mod's own.
@Mixin(value = RandomPos.class, priority = 1100)
public class RandomPosMixin {
    @ModifyExpressionValue(
            method = "generateRandomPosTowardDirection",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/PathfinderMob;getRestrictCenter()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos toroidal$homeBiasThroughSeam(BlockPos center, @Local(argsOnly = true) PathfinderMob mob) {
        return SeamSteering.nearestCopy(mob, center);
    }
}
