package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.phys.Vec3;

// A leaping attacker takes the difference to its target and sets it, normalized, as its own jump velocity. The range
// gate that arms the leap reads distanceToSqr and is already folded, so a target three blocks away through the seam
// does pass it — and the jump is then built from the raw difference, which carries the whole world's magnitude with the
// wrong sign. The mob springs away from the prey it just decided to pounce on.
//
// The difference is folded where it is taken, before vanilla measures it: the lengthSqr guard, the normalize and the
// scale it mixes with the mob's own motion are left as they were, correct now that their input names the copy of the
// target the mob is actually standing next to.
@Mixin(LeapAtTargetGoal.class)
public class LeapAtTargetGoalMixin {
    @Shadow
    @Final
    private Mob mob;

    @WrapOperation(
            method = "start",
            at = @At(value = "NEW", target = "(DDD)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 toroidal$leapDeltaThroughSeam(double x, double y, double z, Operation<Vec3> original) {
        return SeamAim.foldDelta(this.mob, original.call(x, y, z));
    }
}
