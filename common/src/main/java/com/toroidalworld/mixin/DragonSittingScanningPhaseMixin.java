package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonSittingScanningPhase;

@Mixin(DragonSittingScanningPhase.class)
public class DragonSittingScanningPhaseMixin {
    @WrapOperation(
            method = "doServerTick",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_X))
    private double toroidal$scanTargetX(LivingEntity target, Operation<Double> original) {
        return SeamAim.nearestTo(((DragonPhaseAccessor) this).toroidal$dragon(),
                target.position().with(Direction.Axis.X, original.call(target))).x;
    }

    @WrapOperation(
            method = "doServerTick",
            at = @At(value = "INVOKE", target = InjectionTargets.LIVING_ENTITY_GET_Z))
    private double toroidal$scanTargetZ(LivingEntity target, Operation<Double> original) {
        return SeamAim.nearestTo(((DragonPhaseAccessor) this).toroidal$dragon(),
                target.position().with(Direction.Axis.Z, original.call(target))).z;
    }
}
