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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveThroughVillageGoal;

// A mob walking through a village picks a door it has not visited, paths to it, and stops when it is close. Both ends
// of "close" read the door as a plain BlockPos, so a door across the seam is a world away by both of them.
//
// The two readings fail together and reinforce each other: the run never ends of its own accord, and the door is never
// added to the visited list, so the next pick is the same door again. The mob keeps re-walking to a door it is standing
// in front of, and the rest of the village it was meant to move through is never chosen.
//
// Both are the same comparison on the distance through the seam, so both fold in one place.
@Mixin(MoveThroughVillageGoal.class)
public class MoveThroughVillageGoalMixin {
    @Shadow
    @Final
    protected PathfinderMob mob;

    @WrapOperation(
            method = {"canContinueToUse", "stop"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$poiArrivalThroughSeam(BlockPos poiPos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.mob, poiPos, bodyPosition, distance);
    }
}
