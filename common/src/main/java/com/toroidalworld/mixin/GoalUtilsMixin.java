package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.GoalUtils;

@Mixin(GoalUtils.class)
public class GoalUtilsMixin {
    @ModifyExpressionValue(
            method = "mobRestricted",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/PathfinderMob;getHomePosition()Lnet/minecraft/core/BlockPos;"))
    private static BlockPos toroidal$homeFilterThroughSeam(BlockPos home, @Local(argsOnly = true) PathfinderMob mob) {
        return SeamSteering.nearestCopy(mob, home);
    }
}
