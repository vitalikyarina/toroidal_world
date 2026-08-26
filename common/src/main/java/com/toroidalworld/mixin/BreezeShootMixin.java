package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.breeze.Shoot;
import net.minecraft.world.phys.Vec3;

@Mixin(Shoot.class)
public class BreezeShootMixin {
    @WrapOperation(
            method = "isTargetWithinRange",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"))
    private static double toroidal$shootRangeThroughSeam(Vec3 from, Vec3 to, Operation<Double> original,
            @Local(argsOnly = true) Breeze body) {
        return SeamRange.sqr(body, from, to);
    }

    @WrapOperation(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/breeze/Breeze;J)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D"))
    private double toroidal$aimTargetX(LivingEntity target, Operation<Double> original,
            @Local(argsOnly = true) Breeze breeze) {
        return SeamAim.nearestTo(breeze, target.position().with(Direction.Axis.X, original.call(target))).x;
    }

    @WrapOperation(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/monster/breeze/Breeze;J)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D"))
    private double toroidal$aimTargetZ(LivingEntity target, Operation<Double> original,
            @Local(argsOnly = true) Breeze breeze) {
        return SeamAim.nearestTo(breeze, target.position().with(Direction.Axis.Z, original.call(target))).z;
    }
}
