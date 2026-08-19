package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.StayCloseToTarget;
import net.minecraft.world.phys.Vec3;

@Mixin(StayCloseToTarget.class)
public class StayCloseToTargetMixin {
    @WrapOperation(
            method = "lambda$create$2",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"))
    private static boolean toroidal$followReachThroughSeam(Vec3 bodyPosition, Position targetPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return SeamRange.closerThan(body, bodyPosition, targetPosition, distance);
    }
}
