package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.engine.seam.SeamAim;
import com.toroidalworld.engine.seam.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ShulkerBullet;

@Mixin(ShulkerBullet.class)
public class ShulkerBulletMixin {
    @WrapOperation(
            method = "selectNextMoveDirection",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_X))
    private double toroidal$homeOnTargetX(Entity target, Operation<Double> original) {
        return SeamAim.nearestCoord((ShulkerBullet) (Object) this, target, Direction.Axis.X, original.call(target));
    }

    @WrapOperation(
            method = "selectNextMoveDirection",
            at = @At(value = "INVOKE", target = InjectionTargets.ENTITY_GET_Z))
    private double toroidal$homeOnTargetZ(Entity target, Operation<Double> original) {
        return SeamAim.nearestCoord((ShulkerBullet) (Object) this, target, Direction.Axis.Z, original.call(target));
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$targetBlockThroughSeam(BlockPos targetBlock) {
        return SeamSteering.nearestCopy((ShulkerBullet) (Object) this, targetBlock);
    }
}
