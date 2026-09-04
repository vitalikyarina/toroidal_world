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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.LocateHidingPlace;

@Mixin(LocateHidingPlace.class)
public class LocateHidingPlaceMixin {
    @WrapOperation(
            method = { "lambda$create$5", "lambda$create$10" },
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN),
            expect = 2)
    private static boolean toroidal$hidingPlaceReachThroughSeam(BlockPos hidingPos, Position bodyPosition,
            double distance, Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return SeamRange.closerToCenterThan(body, hidingPos, bodyPosition, distance);
    }
}
