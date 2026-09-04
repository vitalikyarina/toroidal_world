package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.animal.Turtle;

@Mixin(targets = "net.minecraft.world.entity.animal.Turtle$TurtleGoHomeGoal")
public class TurtleGoHomeGoalMixin {
    @Shadow
    @Final
    private Turtle turtle;

    @WrapOperation(
            method = { "canUse", "canContinueToUse", "tick" },
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private boolean toroidal$homeReachThroughSeam(BlockPos homePos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.turtle, homePos, bodyPosition, distance);
    }
}
