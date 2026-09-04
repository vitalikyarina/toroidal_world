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
import net.minecraft.world.entity.ai.behavior.SocializeAtBell;

@Mixin(SocializeAtBell.class)
public class SocializeAtBellMixin {
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private static boolean toroidal$meetingPointReachThroughSeam(BlockPos meetingPos, Position bodyPosition,
            double distance, Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return SeamRange.closerToCenterThan(body, meetingPos, bodyPosition, distance);
    }
}
