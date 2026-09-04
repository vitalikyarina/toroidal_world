package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.animal.Turtle;

@Mixin(Turtle.class)
public class TurtleMixin {
    @WrapOperation(
            method = "travel",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private boolean toroidal$homeApproachThroughSeam(BlockPos homePos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        Turtle turtle = (Turtle) (Object) this;
        return SeamRange.closerToCenterThan(turtle, homePos, bodyPosition, distance);
    }
}
