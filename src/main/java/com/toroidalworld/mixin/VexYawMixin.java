package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Vex;

// A vex chasing something turns its body to face it directly, from a raw difference, while the flight itself is steered
// through the move control and already folded. Across the seam the two disagree: the vex flies at its target with its
// back to it.
//
// Wrapped where the yaw is written rather than where the difference is taken: the goal reaches its vex through the
// enclosing instance, so the coordinate reads inside it name no object this can hold. The write does — vanilla's own
// receiver is the vex — and the angle is small enough to state again here. The other call this method makes, for a vex
// with no target, faces its own movement and has nothing to fold.
@Mixin(targets = "net.minecraft.world.entity.monster.Vex$VexMoveControl")
public class VexYawMixin {
    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Vex;setYRot(F)V", ordinal = 1))
    private void toroidal$bodyYawThroughSeam(Vex vex, float yRot, Operation<Void> original) {
        LivingEntity target = vex.getTarget();
        if (target == null) {
            original.call(vex, yRot);
            return;
        }

        double deltaX = SeamAim.foldX(vex, target.getX() - vex.getX());
        double deltaZ = SeamAim.foldZ(vex, target.getZ() - vex.getZ());
        original.call(vex, -((float) Mth.atan2(deltaX, deltaZ)) * (180.0F / (float) Math.PI));
    }
}
