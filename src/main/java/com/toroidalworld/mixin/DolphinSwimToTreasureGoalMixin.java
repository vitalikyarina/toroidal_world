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
import net.minecraft.world.entity.animal.dolphin.Dolphin;

// Fed a fish, a dolphin leads the way to the nearest buried treasure and stops once it is four blocks from it. The
// structure it found sits at its own coordinates in the world and the dolphin is wrapped, so a treasure across the seam
// is never arrived at: the goal cannot end, and stopping does not clear the fish either, so the same swim starts again.
// The dolphin circles the chest it is leading you to.
//
// Only the horizontal gap is asked — the comparison is built at the dolphin's own height — and folding leaves that as
// it was; the seam has no vertical direction to fold.
@Mixin(targets = "net.minecraft.world.entity.animal.dolphin.Dolphin$DolphinSwimToTreasureGoal")
public class DolphinSwimToTreasureGoalMixin {
    @Shadow
    @Final
    private Dolphin dolphin;

    @WrapOperation(
            method = { "canContinueToUse", "stop" },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"),
            expect = 2)
    private boolean toroidal$treasureReachThroughSeam(BlockPos treasurePos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(this.dolphin, treasurePos, bodyPosition, distance);
    }
}
