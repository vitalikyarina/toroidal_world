package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.GoToTargetLocation;

// The arrival test for a remembered location — where a raid was won, where a celebration is held. Read raw from the far
// side of the seam it never says yes, so the mob keeps setting itself a fresh walk and look target one tick after
// another around a place it is already standing in, and never settles into what it came here to do.
@Mixin(GoToTargetLocation.class)
public class GoToTargetLocationMixin {
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"))
    private static boolean toroidal$arrivalThroughSeam(BlockPos location, Vec3i bodyPos, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) Mob body) {
        return SeamRange.closerThan(body, location, bodyPos, distance);
    }
}
