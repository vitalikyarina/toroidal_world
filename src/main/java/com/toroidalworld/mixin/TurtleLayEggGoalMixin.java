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

// Laying is allowed only within nine blocks of the home beach, asked once to begin and once every tick to continue. The
// sand the turtle walks to is found by the block goal underneath this one, whose own arrival is already folded — so
// across the seam the turtle reaches the right sand and is then refused by the gate above it.
//
// A turtle carrying an egg stands on its own beach and is told it is a world from home, so it never digs; the egg is
// carried until the turtle finds a beach far enough from the boundary, which on a small world may be nowhere.
@Mixin(targets = "net.minecraft.world.entity.animal.turtle.Turtle$TurtleLayEggGoal")
public class TurtleLayEggGoalMixin {
    @Shadow
    @Final
    private Turtle turtle;

    @WrapOperation(
            method = { "canUse", "canContinueToUse" },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$nestingRangeThroughSeam(BlockPos homePos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.turtle, homePos, bodyPosition, distance);
    }
}
