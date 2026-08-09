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
import net.minecraft.world.entity.animal.Bee;

// Every range a bee measures goes through one method on the bee, which folds the remembered position before comparing —
// except this one. Entering the hive asks the two-block question directly on the hive position, so it is the single
// reading that fold does not reach.
//
// The bee therefore flies home correctly, hovers at the entrance and never goes in: no honey delivered, no nectar
// cleared, and it stays out through the night and the rain.
@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeeEnterHiveGoal")
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
