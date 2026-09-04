package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.ai.behavior.ChargeAttack;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;

@Mixin(ChargeAttack.class)
public class ChargeAttackMixin {
    @WrapOperation(
            method = "canStillUse(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/Animal;J)Z",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT))
    private Vec3 toroidal$chargeGuardThroughSeam(Vec3 from, Vec3 to, Operation<Vec3> original,
            @Local(argsOnly = true) Animal body) {
        return SeamAim.foldDelta(body, original.call(from, to));
    }

    @WrapOperation(
            method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/Animal;J)V",
            at = @At(value = "INVOKE", target = InjectionTargets.VEC3_SUBTRACT))
    private Vec3 toroidal$chargeHeadingThroughSeam(Vec3 from, Vec3 to, Operation<Vec3> original,
            @Local(argsOnly = true) Animal body) {
        return SeamAim.foldDelta(body, original.call(from, to));
    }
}
