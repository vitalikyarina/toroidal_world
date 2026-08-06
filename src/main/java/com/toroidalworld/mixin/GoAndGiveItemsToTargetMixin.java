package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.GoAndGiveItemsToTarget;
import net.minecraft.world.phys.Vec3;

// The carrier walks its item to somebody and throws it once it is within three blocks. The walk is fine; the throw is
// gated on a raw distance between the deposit point and the carrier's own eyes, so across the seam an allay hovers over
// the player it fetched for and holds onto the item forever — the pickup cooldown that would let it fetch anything else
// is only set when the throw happens.
@Mixin(GoAndGiveItemsToTarget.class)
public class GoAndGiveItemsToTargetMixin {
    @WrapOperation(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;J)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"))
    private double toroidal$depositDistanceThroughSeam(Vec3 depositPosition, Vec3 eyePosition,
            Operation<Double> original, @Local(argsOnly = true) LivingEntity body) {
        return Math.sqrt(SeamRange.sqr(body, depositPosition, eyePosition));
    }
}
