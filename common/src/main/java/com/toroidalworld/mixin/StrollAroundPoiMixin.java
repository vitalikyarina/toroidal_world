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
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.StrollAroundPoi;

// The same tether as the walk to a POI, guarding the idle wander around one instead. Read raw across the seam it holds
// nobody: the villager that lives beside the boundary is judged to be nowhere near its own village and stops milling
// about it, which is the whole of what a bell or a job site looks like from outside.
//
// Where it wanders to is already folded — the candidate comes from the random-position family — so the gate is the only
// thing between a villager on the far side and its ordinary day.
@Mixin(StrollAroundPoi.class)
public class StrollAroundPoiMixin {
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private static boolean toroidal$poiTetherThroughSeam(BlockPos poiPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) PathfinderMob body) {
        return ReseatProbe.decided(body.level(), ReseatProbe.POI_TETHER_AROUND,
                original.call(poiPos, bodyPosition, distance),
                SeamRange.closerToCenterThan(body, poiPos, bodyPosition, distance));
    }
}
