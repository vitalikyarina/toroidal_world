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
import net.minecraft.world.level.block.entity.BellBlockEntity;

@Mixin(BellBlockEntity.class)
public class BellBlockEntityMixin {
    @WrapOperation(
            method = "updateEntities",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private boolean toroidal$hearingRangeThroughSeam(BlockPos bellPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local LivingEntity body) {
        return SeamRange.closerToCenterThan(body, bellPos, bodyPosition, distance);
    }

    @WrapOperation(
            method = { "areRaidersNearby", "isRaiderWithinRange", "lambda$showBellParticles$0" },
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN),
            expect = 3)
    private static boolean toroidal$raiderRangeThroughSeam(BlockPos bellPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local LivingEntity body) {
        return SeamRange.closerToCenterThan(body, bellPos, bodyPosition, distance);
    }
}
