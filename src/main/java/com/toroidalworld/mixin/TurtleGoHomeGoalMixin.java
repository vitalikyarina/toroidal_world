package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.animal.turtle.Turtle;

// The whole journey home, gated three times over on the same raw difference: sixty-four blocks decides whether to set
// off at all, seven decides when the turtle has arrived and the goal may end, and sixteen counts the ticks it has been
// close enough — the timer that gives up and marks the turtle stuck.
//
// Across the seam all three read a world out. A turtle whose beach lies over the boundary sets off whenever the random
// roll allows and never stops: it is never within seven blocks, so the goal runs until the six-hundred-tick patience
// expires, and because it is never within sixteen either, that counter does not advance and the patience never runs
// down. It swims home forever, which also means it never lays.
//
// Where it swims to is already folded — the destination is handed to the random-position family — so these three
// readings are the whole of what is left.
@Mixin(targets = "net.minecraft.world.entity.animal.turtle.Turtle$TurtleGoHomeGoal")
public class TurtleGoHomeGoalMixin {
    @Shadow
    @Final
    private Turtle turtle;

    @WrapOperation(
            method = { "canUse", "canContinueToUse", "tick" },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$homeReachThroughSeam(BlockPos homePos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.turtle, homePos, bodyPosition, distance);
    }
}
