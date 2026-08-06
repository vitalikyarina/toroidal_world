package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.ShulkerBullet;

// The bullet does not fly at its target, it steps toward it: each leg picks a single face to travel along, and the face
// is picked by comparing the bullet's block against the target's. Across the seam that comparison reads a target on the
// far side as lying the other way, so the bullet chooses the opposite face, and the homing delta it finally builds from
// the same pair confirms the mistake. A shulker either side of the seam is harmless.
//
// The whole method hangs off one reading of the target's position, taken before the block it steps toward is rounded
// out of it. Folding there — rather than on the deltas at the end — leaves the face choice, the two block gate that
// decides whether to steer at all, and the delta all naming the same copy of the world.
//
// A bullet whose way is blocked gets one more chance to turn: it asks whether it has drawn level with the target on the
// axis it travels, comparing whole block coordinates for equality. Equality is the one test a fold cannot approximate —
// across the seam the two numbers are a world apart and never meet, so the bullet holds a course into the wall it is
// already stopped by. That reading is folded too, as a block rather than a point.
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
