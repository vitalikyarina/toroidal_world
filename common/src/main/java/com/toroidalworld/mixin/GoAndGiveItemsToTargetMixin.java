package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.GoAndGiveItemsToTarget;
import net.minecraft.world.phys.Vec3;

@Mixin(GoAndGiveItemsToTarget.class)
public class GoAndGiveItemsToTargetMixin {
    @WrapOperation(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;J)V",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO))
    private double toroidal$depositDistanceThroughSeam(Vec3 depositPosition, Vec3 eyePosition,
            Operation<Double> original, @Local(argsOnly = true) LivingEntity body) {
        return Math.sqrt(SeamRange.sqr(body, depositPosition, eyePosition));
    }
}
