package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.ShulkerBullet;

@Mixin(ShulkerBullet.class)
public class ShulkerBulletMixin {
    @ModifyExpressionValue(
            method = "selectNextMoveDirection",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double toroidal$homeOnTargetX(double targetX) {
        return SeamAim.nearX((ShulkerBullet) (Object) this, targetX);
    }

    @ModifyExpressionValue(
            method = "selectNextMoveDirection",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double toroidal$homeOnTargetZ(double targetZ) {
        return SeamAim.nearZ((ShulkerBullet) (Object) this, targetZ);
    }

    @ModifyExpressionValue(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    private BlockPos toroidal$targetBlockThroughSeam(BlockPos targetBlock) {
        ShulkerBullet self = (ShulkerBullet) (Object) this;
        WorldLoopTransformer transformer = ((TransformerSource) self).toroidal$wrappedTransformer();
        if (transformer == null) {
            return targetBlock;
        }

        return transformer.blocks.nearestCopy(self.blockPosition(), targetBlock);
    }
}
