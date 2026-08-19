package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.raid.Raider;

@Mixin(targets = "net.minecraft.world.entity.raid.Raider$RaiderMoveThroughVillageGoal")
public class RaiderMoveThroughVillageGoalMixin {
    @Shadow
    @Final
    private Raider raider;

    @WrapOperation(
            method = { "canContinueToUse", "stop" },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$houseReachThroughSeam(BlockPos housePos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.raider, housePos, bodyPosition, distance);
    }
}
