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
import net.minecraft.world.entity.ai.behavior.SleepInBed;

@Mixin(SleepInBed.class)
public class SleepInBedMixin {
    @WrapOperation(
            method = { "checkExtraStartConditions", "canStillUse" },
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private boolean toroidal$bedReachThroughSeam(BlockPos bedPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return SeamRange.closerToCenterThan(body, bedPos, bodyPosition, distance);
    }
}
