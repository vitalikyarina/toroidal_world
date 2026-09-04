package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;

@Mixin(CrossbowItem.class)
public class CrossbowItemAimMixin {
    @WrapOperation(
            method = "shootProjectile",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X, ordinal = 0))
    private double toroidal$aimTargetX(LivingEntity target, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) LivingEntity shooter) {
        return SeamAim.nearestTo(shooter, target.position().with(Direction.Axis.X, original.call(target))).x;
    }

    @WrapOperation(
            method = "shootProjectile",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z, ordinal = 0))
    private double toroidal$aimTargetZ(LivingEntity target, Operation<Double> original,
            @Local(argsOnly = true, ordinal = 0) LivingEntity shooter) {
        return SeamAim.nearestTo(shooter, target.position().with(Direction.Axis.Z, original.call(target))).z;
    }
}
