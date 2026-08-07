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

// The one behaviour every walk target in the game passes through: whatever set it — a bed, a job site, a fleeing
// villager's escape route — this is what turns it into a path and decides when the mob has arrived. Both of its own
// readings compare the target's coordinates against the mob's, and both are raw.
//
// Arrival errs the safe way across the seam, since a target read a world off is never "reached", so the mob keeps
// walking and the state rights itself once it crosses. What it costs is the shortcut: a mob already standing within
// reach of its target is sent to compute a path to where it is, and the erase that would have ended the behaviour
// waits for the crossing. The retarget check pays worse — a walk target that moves a step across the boundary reads as
// a whole world of movement and forces a full repath every tick it does so.
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
