package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ShulkerBullet;

@Mixin(ShulkerBullet.class)
public class ShulkerBulletMixin {
    @WrapOperation(
            method = "selectNextMoveDirection",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double toroidal$homeOnTargetX(Entity target, Operation<Double> original) {
        return SeamAim.nearestTo((ShulkerBullet) (Object) this, target.position()).x;
    }

    @WrapOperation(
            method = "selectNextMoveDirection",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double toroidal$homeOnTargetZ(Entity target, Operation<Double> original) {
        return SeamAim.nearestTo((ShulkerBullet) (Object) this, target.position()).z;
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$targetBlockThroughSeam(BlockPos targetBlock) {
        ShulkerBullet self = (ShulkerBullet) (Object) this;
        WorldFold transformer = ((TransformerSource) self).toroidal$wrappedTransformer();
        if (transformer == null) {
            return targetBlock;
        }

        return transformer.nearestCopy(self.blockPosition(), targetBlock);
    }
}
