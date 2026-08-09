package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.ai.behavior.AssignProfessionFromJobSite;
import net.minecraft.world.entity.npc.Villager;

// The moment a villager takes a trade: it has claimed a workstation, walked to it, and touching it turns the claim into
// a profession. Two blocks is the whole of the test, and it is a raw difference against the claimed position.
//
// Across the seam the villager arrives and the claim never converts. It keeps the potential job site, so the acquire
// behaviour will not look for another, and it stands at an unemployed lectern for good.
@Mixin(AssignProfessionFromJobSite.class)
public class AssignProfessionFromJobSiteMixin {
    @WrapOperation(
            method = "lambda$create$2",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private static boolean toroidal$jobSiteReachThroughSeam(BlockPos jobSitePos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) Villager body) {
        return SeamRange.closerToCenterThan(body, jobSitePos, bodyPosition, distance);
    }
}
