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
import net.minecraft.world.entity.animal.bee.Bee;

@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeeEnterHiveGoal")
public class BeeEnterHiveGoalMixin {
    @Shadow(aliases = "this$0")
    @Final
    private Bee bee;

    @WrapOperation(
            method = "canBeeUse",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$hiveEntranceThroughSeam(BlockPos hivePos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.bee, hivePos, bodyPosition, distance);
    }
}
