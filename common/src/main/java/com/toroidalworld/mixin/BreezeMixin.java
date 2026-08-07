package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamAim;

import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.phys.Vec3;

// The inner ring the breeze keeps its distance by. The reading is Vec3.closerThan — a Vec3 method, so none of the five
// Entity distance folds reach it, and through the seam a target four blocks away reads as a world away: the ring is
// never entered, and the whole branch of the slide that repositions inside it is dead.
//
// The target becomes its copy nearest the breeze and the ring reads its true 4-block horizontal, 10-block vertical
// range. Vanilla compares against the centre of the breeze's own block rather than its exact position; that is under a
// block of difference and cannot change which copy is the nearest one.
@Mixin(Breeze.class)
public class BreezeMixin {
    @ModifyVariable(method = "withinInnerCircleRange", at = @At("HEAD"), argsOnly = true)
    private Vec3 toroidal$innerRingTargetThroughSeam(Vec3 target) {
        return SeamAim.nearestTo((Breeze) (Object) this, target);
    }
}
