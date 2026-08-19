package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;

@Mixin(MoveToTargetSink.class)
public class MoveToTargetSinkMixin {
    @WrapOperation(
            method = "reachedTarget",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distManhattan(Lnet/minecraft/core/Vec3i;)I"))
    private int toroidal$arrivalDistanceThroughSeam(BlockPos targetPos, Vec3i bodyPos, Operation<Integer> original,
            @Local(argsOnly = true) Mob body) {
        return SeamRange.manhattan(body, targetPos, bodyPos);
    }

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"))
    private double toroidal$retargetDistanceThroughSeam(BlockPos targetPos, Vec3i lastTargetPos,
            Operation<Double> original, @Local(argsOnly = true) Mob body) {
        return SeamRange.sqr(body, targetPos, lastTargetPos);
    }
}
