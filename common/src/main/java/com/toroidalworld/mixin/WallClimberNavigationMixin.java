package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;

@Mixin(WallClimberNavigation.class)
public class WallClimberNavigationMixin {
    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.BLOCK_POS_CLOSER_TO_CENTER_THAN))
    private boolean toroidal$climbTargetReachThroughSeam(BlockPos climbPos, Position bodyPosition, double distance,
            Operation<Boolean> original) {
        return SeamRange.closerToCenterThan(((PathNavigationAccessor) this).toroidal$mob(), climbPos, bodyPosition,
                distance);
    }
}
