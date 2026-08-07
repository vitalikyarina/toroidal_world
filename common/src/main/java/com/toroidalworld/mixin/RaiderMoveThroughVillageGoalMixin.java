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
import net.minecraft.world.entity.raid.Raider;

// How a raid sweeps a village: each raider takes a house it has not visited yet, walks to it, and marks it visited on
// arrival. Both readings are raw differences against the house — one ends the walk, the other decides whether to write
// the house down as done.
//
// Across the seam neither fires. The raider never arrives, so it never marks the house, so the same house is picked
// again as the nearest unvisited one — a raid on a village at the boundary walks the same doorstep until the raid times
// out, instead of searching the houses out.
@Mixin(targets = "net.minecraft.world.entity.raid.Raider$RaiderMoveThroughVillageGoal")
public class RaiderMoveThroughVillageGoalMixin {
    @Shadow
    @Final
    private Raider raider;

    @WrapOperation(
            method = { "canContinueToUse", "stop" },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$houseReachThroughSeam(BlockPos housePos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.raider, housePos, bodyPosition, distance);
    }
}
