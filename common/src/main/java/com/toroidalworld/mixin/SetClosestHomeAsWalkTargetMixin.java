package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.SetClosestHomeAsWalkTarget;

@Mixin(SetClosestHomeAsWalkTarget.class)
public class SetClosestHomeAsWalkTargetMixin {
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_DIST_SQR))
    private static double toroidal$homeDistanceThroughSeam(BlockPos homePos, Vec3i bodyPos, Operation<Double> original,
            @Local(argsOnly = true) PathfinderMob body) {
        return SeamRange.sqr(body, homePos, bodyPos);
    }
}
