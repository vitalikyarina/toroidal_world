package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.probe.ReseatProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.StayCloseToTarget;
import net.minecraft.world.phys.Vec3;

// Following at a distance, inverted: the behaviour does nothing while the mob is already near enough, and only past
// that gate adopts the target as somewhere to walk. Read raw, a mob on the far side of the seam is never near enough,
// so it adopts a walk target it is standing beside and keeps re-adopting it; read raw from the other direction, the
// gate is the only thing that would have started it moving at all.
@Mixin(StayCloseToTarget.class)
public class StayCloseToTargetMixin {
    @WrapOperation(
            method = "lambda$create$0",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"))
    private static boolean toroidal$followReachThroughSeam(Vec3 bodyPosition, Position targetPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return ReseatProbe.decided(body.level(), ReseatProbe.FOLLOW_REACH,
                original.call(bodyPosition, targetPosition, distance),
                SeamRange.closerThan(body, bodyPosition, targetPosition, distance));
    }
}
