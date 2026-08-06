package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.ai.behavior.ChargeAttack;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;

// A charge is aimed once and then flown blind: the heading is taken at the start and set as the whole of the mob's
// movement until something ends the run. Every reading it rests on is a plain difference between two positions.
//
// The heading itself comes out reversed across the seam, so the charge leaves in the opposite direction. The two guards
// that would end the run are wrong in opposite ways: how far the mob has come is measured from a start position it may
// have wrapped away from, and how far the target still is reads a world off — either one exceeds its limit at once, so
// a charge that did start is abandoned on the next tick.
//
// All three are the difference itself, so each is folded where it is taken and the length, the square and the normalize
// that vanilla derives from it are left alone.
@Mixin(ChargeAttack.class)
public class ChargeAttackMixin {
    @WrapOperation(
            method = "canStillUse(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/Animal;J)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$chargeGuardThroughSeam(Vec3 from, Vec3 to, Operation<Vec3> original,
            @Local(argsOnly = true) Animal body) {
        return SeamAim.foldDelta(body, original.call(from, to));
    }

    @WrapOperation(
            method = "start(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/Animal;J)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$chargeHeadingThroughSeam(Vec3 from, Vec3 to, Operation<Vec3> original,
            @Local(argsOnly = true) Animal body) {
        return SeamAim.foldDelta(body, original.call(from, to));
    }
}
