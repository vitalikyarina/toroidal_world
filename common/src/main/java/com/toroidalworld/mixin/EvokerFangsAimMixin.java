package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;

// The evoker's fangs are not thrown at anything: they are laid along a line from the evoker outward, and the only thing
// the target contributes is the angle of that line. Across the seam the raw difference gives its opposite, so the fangs
// walk away from the player.
//
// Only the angle is at stake here, and a difference is measured the short way without needing to know where either end
// is — which is what this asks for, because the goal reaches its evoker through the enclosing instance and has no
// reference position to hand. The spell's own range gate is unaffected: it goes through Entity.distanceToSqr.
@Mixin(targets = "net.minecraft.world.entity.monster.illager.Evoker$EvokerAttackSpellGoal")
public class EvokerFangsAimMixin {
    @WrapOperation(
            method = "performSpellCasting",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;atan2(DD)D"))
    private double toroidal$fangAngleThroughSeam(double deltaZ, double deltaX, Operation<Double> original,
            @Local LivingEntity target) {
        return original.call(SeamAim.foldZ(target, deltaZ), SeamAim.foldX(target, deltaX));
    }
}
