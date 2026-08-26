package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Ghast;

@Mixin(targets = "net.minecraft.world.entity.monster.Ghast$GhastLookGoal")
public class GhastFacingAimMixin {
    @Shadow
    @Final
    private Ghast ghast;

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D"))
    private double toroidal$aimTargetX(LivingEntity target, Operation<Double> original) {
        return SeamAim.nearestTo(this.ghast, target.position()).x;
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D"))
    private double toroidal$aimTargetZ(LivingEntity target, Operation<Double> original) {
        return SeamAim.nearestTo(this.ghast, target.position()).z;
    }
}
