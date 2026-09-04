package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;

@Mixin(targets = "net.minecraft.world.entity.monster.Blaze$BlazeAttackGoal")
public class BlazeAimMixin {
    @Shadow
    @Final
    private Blaze blaze;

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private double toroidal$aimTargetX(LivingEntity target, Operation<Double> original) {
        return SeamAim.nearestTo(this.blaze, target.position().with(Direction.Axis.X, original.call(target))).x;
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private double toroidal$aimTargetZ(LivingEntity target, Operation<Double> original) {
        return SeamAim.nearestTo(this.blaze, target.position().with(Direction.Axis.Z, original.call(target))).z;
    }
}
