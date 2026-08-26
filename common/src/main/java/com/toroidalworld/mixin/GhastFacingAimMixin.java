package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ghast;

@Mixin(Ghast.class)
public class GhastFacingAimMixin {
    @WrapOperation(
            method = "faceMovementDirection(Lnet/minecraft/world/entity/Mob;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D"))
    private static double toroidal$aimTargetX(LivingEntity target, Operation<Double> original,
            @Local(argsOnly = true) Mob ghast) {
        return SeamAim.nearestTo(ghast, target.position()).x;
    }

    @WrapOperation(
            method = "faceMovementDirection(Lnet/minecraft/world/entity/Mob;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D"))
    private static double toroidal$aimTargetZ(LivingEntity target, Operation<Double> original,
            @Local(argsOnly = true) Mob ghast) {
        return SeamAim.nearestTo(ghast, target.position()).z;
    }
}
