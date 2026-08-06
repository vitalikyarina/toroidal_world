package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.SetHiddenState;

// Hiding is counted, not observed: the villager only banks a tick towards its stay indoors while this gate says it is
// near its hiding place. Read raw across the seam the counter never moves, so the villager waits out the whole
// three-hundred-tick (15 s) timeout instead, and the panic ends because the clock ran out rather than because it was
// safe — the bell that sent it inside has to sound again before it will hide at all.
@Mixin(SetHiddenState.class)
public class SetHiddenStateMixin {
    @WrapOperation(
            method = "lambda$create$2",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"))
    private static boolean toroidal$hidingReachThroughSeam(BlockPos hidePos, Vec3i bodyPos, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return SeamRange.closerThan(body, hidePos, bodyPos, distance);
    }
}
