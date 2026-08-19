package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ghast;

@Mixin(Ghast.class)
public class GhastFacingAimMixin {
    @ModifyExpressionValue(
            method = "faceMovementDirection(Lnet/minecraft/world/entity/Mob;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D"))
    private static double toroidal$aimTargetX(double targetX, @Local(argsOnly = true) Mob ghast) {
        return SeamAim.nearX(ghast, targetX);
    }

    @ModifyExpressionValue(
            method = "faceMovementDirection(Lnet/minecraft/world/entity/Mob;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D"))
    private static double toroidal$aimTargetZ(double targetZ, @Local(argsOnly = true) Mob ghast) {
        return SeamAim.nearZ(ghast, targetZ);
    }
}
