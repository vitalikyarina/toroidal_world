package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;

// Breaking a door runs while the zombie is within two blocks of it. That reading is the door as a plain BlockPos, so a
// door reached through the seam is two steps away in the world and a world away in the arithmetic: the goal is armed,
// starts, and abandons itself on the next tick, over and over, so the door never takes any damage.
//
// The neighbouring goal on the same door is already right — DoorInteractGoal asks distanceToSqr of the mob, which is
// folded. A zombie at the boundary can therefore open a door it cannot break, which is the odd half-working state this
// removes.
@Mixin(BreakDoorGoal.class)
public class BreakDoorGoalMixin {
    @WrapOperation(
            method = "canContinueToUse",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$doorRangeThroughSeam(BlockPos doorPos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(((DoorInteractGoalAccessor) this).toroidal$mob(), doorPos,
                bodyPosition, distance);
    }
}
