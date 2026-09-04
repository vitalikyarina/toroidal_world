package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.StrollToPoi;

@Mixin(StrollToPoi.class)
public class StrollToPoiMixin {
    @WrapOperation(
            method = "lambda$create$2",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private static boolean toroidal$poiTetherThroughSeam(BlockPos poiPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) PathfinderMob body) {
        return SeamRange.closerToCenterThan(body, poiPos, bodyPosition, distance);
    }
}
