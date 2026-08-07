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
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;

// Everything a mob walks to a particular block for comes through here — a turtle to the sand it hatched on, a cat to a
// bed or a chest, a fox and a rabbit to a garden, a strider to lava, a drowned to a nether portal. The block is chosen
// by offsets walked out from the mob, so one found near the boundary keeps the coordinate that walk arrived at and is
// remembered outside the world; the mob's own position never is.
//
// Getting there works: the navigation takes the target as the copy nearest the mob. Arriving does not. Standing on that
// very block the mob compares the two names of it and finds them a world apart, so it never registers as having
// arrived, and it stands on its own destination trying to reach it until the goal gives up. The turtle never lays, the
// cat never sits.
//
// The gate is restated on the distance through the seam rather than the block being moved into the world, because the
// mob crosses the boundary on the way and is wrapped there: a folded difference is right on both sides of that, where a
// restated block would have to be restated again after every wrap.
@Mixin(MoveToBlockGoal.class)
public class MoveToBlockGoalMixin {
    @Shadow
    @Final
    protected PathfinderMob mob;

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$arrivalThroughSeam(BlockPos moveToTarget, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.mob, moveToTarget, bodyPosition, distance);
    }
}
