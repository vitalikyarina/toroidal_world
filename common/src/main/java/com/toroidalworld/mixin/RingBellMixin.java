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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.RingBell;

@Mixin(RingBell.class)
public class RingBellMixin {
    @WrapOperation(
            method = "lambda$create$2",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_THAN))
    private static boolean toroidal$bellReachThroughSeam(BlockPos bellPos, Vec3i bodyPos, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return SeamRange.closerThan(body, bellPos, bodyPos, distance);
    }
}
