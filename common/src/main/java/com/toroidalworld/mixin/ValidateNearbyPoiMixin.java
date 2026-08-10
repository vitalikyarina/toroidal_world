package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.probe.ReseatProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.ValidateNearbyPoi;

// The housekeeping pass that forgets a home or a job site once it is gone — the block broken, the bed taken by someone
// else. It only looks when the place is within sixteen blocks, on the reasoning that a villager cannot know about a POI
// it is nowhere near.
//
// Across the seam that range never opens, so the check is skipped for exactly the villagers standing next to the POI in
// question. The memory outlives the block: a villager keeps a claim on a bed that is no longer there, walks to it every
// night, and no other villager can take it because the claim was never released.
@Mixin(ValidateNearbyPoi.class)
public class ValidateNearbyPoiMixin {
    @WrapOperation(
            method = "lambda$create$0",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private static boolean toroidal$poiInRangeThroughSeam(BlockPos poiPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return ReseatProbe.decided(body.level(), ReseatProbe.POI_IN_RANGE,
                original.call(poiPos, bodyPosition, distance),
                SeamRange.closerToCenterThan(body, poiPos, bodyPosition, distance));
    }
}
