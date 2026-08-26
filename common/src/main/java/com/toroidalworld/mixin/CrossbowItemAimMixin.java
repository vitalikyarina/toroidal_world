package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;

@Mixin(CrossbowItem.class)
public class CrossbowItemAimMixin {
    @WrapOperation(
            method = "shootProjectile",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private double toroidal$aimTargetX(LivingEntity target, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) LivingEntity shooter) {
        return SeamAim.nearestTo(shooter, target.position()).x;
    }

    @WrapOperation(
            method = "shootProjectile",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double toroidal$aimTargetZ(LivingEntity target, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) LivingEntity shooter) {
        return SeamAim.nearestTo(shooter, target.position()).z;
    }
}
