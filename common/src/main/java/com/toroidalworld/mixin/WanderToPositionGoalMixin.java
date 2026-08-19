package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.WanderingTrader;

@Mixin(targets = "net.minecraft.world.entity.npc.WanderingTrader$WanderToPositionGoal")
public class WanderToPositionGoalMixin {
    @Shadow
    @Final
    private WanderingTrader trader;

    @ModifyExpressionValue(
            method = { "canUse", "tick" },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/npc/WanderingTrader;"
                            + "getWanderTarget()Lnet/minecraft/core/BlockPos;"))
    private @Nullable BlockPos toroidal$wanderTargetThroughSeam(@Nullable BlockPos wanderTarget) {
        return wanderTarget == null ? null : SeamSteering.nearestCopy(this.trader, wanderTarget);
    }
}
