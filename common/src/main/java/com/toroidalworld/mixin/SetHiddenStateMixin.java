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
import net.minecraft.world.entity.ai.behavior.SetHiddenState;

@Mixin(SetHiddenState.class)
public class SetHiddenStateMixin {
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_THAN))
    private static boolean toroidal$hidingReachThroughSeam(BlockPos hidePos, Vec3i bodyPos, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return SeamRange.closerThan(body, hidePos, bodyPos, distance);
    }
}
