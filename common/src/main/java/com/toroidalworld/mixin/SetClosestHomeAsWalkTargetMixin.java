package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.toroidalworld.probe.ReseatProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.SetClosestHomeAsWalkTarget;

// The POI search that finds the nearest home already measures through the seam, so what it hands back may sit just past
// the boundary — and the very next line, which asks whether the mob is already standing on it, does not. A mob home
// across the seam is therefore never "already here": every second it pays for a fresh POI sweep, a batch of candidate
// paths and a walk target set to the ground under its own feet.
@Mixin(SetClosestHomeAsWalkTarget.class)
public class SetClosestHomeAsWalkTargetMixin {
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"))
    private static double toroidal$homeDistanceThroughSeam(BlockPos homePos, Vec3i bodyPos, Operation<Double> original,
            @Local(argsOnly = true) PathfinderMob body) {
        return ReseatProbe.decided(body.level(), ReseatProbe.HOME_DISTANCE, "blocks_sqr",
                original.call(homePos, bodyPos),
                SeamRange.sqr(body, homePos, bodyPos));
    }
}
