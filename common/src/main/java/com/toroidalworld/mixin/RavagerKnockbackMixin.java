package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ravager;

@Mixin(Ravager.class)
public class RavagerKnockbackMixin {
    @WrapOperation(
            method = "strongKnockback(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X))
    private double toroidal$shoveTargetNearX(Entity target, Operation<Double> original) {
        return SeamAim.nearestTo((Entity) (Object) this,
                target.position().with(Direction.Axis.X, original.call(target))).x;
    }

    @WrapOperation(
            method = "strongKnockback(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z))
    private double toroidal$shoveTargetNearZ(Entity target, Operation<Double> original) {
        return SeamAim.nearestTo((Entity) (Object) this,
                target.position().with(Direction.Axis.Z, original.call(target))).z;
    }
}
