package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.SleepInBed;

// Two blocks to lie down, and 1.14 to stay asleep — both raw differences against the bed the villager claimed, which is
// remembered at the position it occupies in the world while the villager is wherever the wrap funnel left it. A bed
// across the seam is therefore never reached: the villager walks to it, stands on it, and the behaviour refuses to
// start, so it stays up all night with the door open behind it.
//
// The second reading is the one that would wake it again. Folding only the first would have a villager go to sleep and
// be thrown out of bed on the very next tick, which is why both are taken here.
@Mixin(SleepInBed.class)
public class SleepInBedMixin {
    @WrapOperation(
            method = { "checkExtraStartConditions", "canStillUse" },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$bedReachThroughSeam(BlockPos bedPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity body) {
        return SeamRange.closerToCenterThan(body, bedPos, bodyPosition, distance);
    }
}
