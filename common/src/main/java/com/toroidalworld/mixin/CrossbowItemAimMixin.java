package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;

@Mixin(CrossbowItem.class)
public class CrossbowItemAimMixin {
    @ModifyExpressionValue(
            method = "shootProjectile",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D", ordinal = 0))
    private double toroidal$aimTargetX(double targetX, @Local(argsOnly = true, ordinal = 0) LivingEntity shooter) {
        return SeamAim.nearX(shooter, targetX);
    }

    @ModifyExpressionValue(
            method = "shootProjectile",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D", ordinal = 0))
    private double toroidal$aimTargetZ(double targetZ, @Local(argsOnly = true, ordinal = 0) LivingEntity shooter) {
        return SeamAim.nearZ(shooter, targetZ);
    }
}
