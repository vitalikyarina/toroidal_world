package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.FollowMobGoal;

// The one goal that measures its own distance instead of asking the entity for it: three subtractions and a sum, where
// every other follower calls distanceToSqr and is folded already. Across the seam the sum carries the width of the
// world, so the parrot tailing a mob never crosses into the half of the method that says it has arrived — it re-paths
// every ten ticks toward something it is already sitting beside, and never settles.
//
// Past that gate the same difference is taken again to back away when the followed mob crowds it, and read raw it backs
// away along the wrong axis, into the boundary rather than off it.
//
// Both readings, and the gate itself, are built from the followed mob's coordinates, which is what makes this foldable
// without touching a single local: the coordinates arrive through Mob.getX and Mob.getZ, and the receiver of each call
// says whose they are. The goal's own mob is the frame everything is measured in, so it comes through untouched; only
// what it is looking at is moved to its nearest copy.
@Mixin(FollowMobGoal.class)
public class FollowMobGoalMixin {
    @Shadow
    @Final
    private Mob mob;

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getX()D"))
    private double toroidal$followedXThroughSeam(Mob read, Operation<Double> original) {
        double x = original.call(read);
        return read == this.mob ? x : SeamAim.nearX(this.mob, x);
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;getZ()D"))
    private double toroidal$followedZThroughSeam(Mob read, Operation<Double> original) {
        double z = original.call(read);
        return read == this.mob ? z : SeamAim.nearZ(this.mob, z);
    }

    // The back-away has a second door into it, for the mob that is close but not yet crowded: it is let through when the
    // one it follows is already looking straight at it, asked as an equality between that mob's wanted look point and
    // its own position. The wanted point belongs to the other mob and is stated in the frame that mob stands in, so
    // across the seam it can never equal a coordinate on this side, and the arm is reachable only by the crowding test
    // beside it.
    //
    // The point is restated where the position it is compared against is read, which is the goal's own mob; the height
    // is compared too and comes through untouched, having no seam to cross.
    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/control/LookControl;getWantedX()D"))
    private double toroidal$wantedLookXThroughSeam(LookControl lookControl, Operation<Double> original) {
        return SeamAim.nearX(this.mob, original.call(lookControl));
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/ai/control/LookControl;getWantedZ()D"))
    private double toroidal$wantedLookZThroughSeam(LookControl lookControl, Operation<Double> original) {
        return SeamAim.nearZ(this.mob, original.call(lookControl));
    }
}
