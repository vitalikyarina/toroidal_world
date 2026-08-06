package com.toroidalworld.mixin;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;

// The trader is given somewhere to be — the place it was summoned near, or the far side of the world it is leaving for
// — and this goal walks it there. Three questions are asked of that destination and all three subtract it raw from the
// trader: whether to set out at all, whether to walk straight at it or ten blocks in its direction, and, on the latter
// arm, what that direction is.
//
// Across the seam the first two always answer "still far", which forces the third, and the third points the long way
// round the world: the trader sets off away from a destination it is standing next to and keeps at it, because it can
// never arrive.
//
// One read serves all three. The destination becomes its copy nearest the trader where the goal fetches it, so the two
// range questions measure the ground the trader would actually cover and the heading is the short way through; the near
// arm then hands the same copy to the navigation, which unwraps toward the mob as it always did.
@Mixin(targets = "net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader$WanderToPositionGoal")
public class WanderToPositionGoalMixin {
    @Shadow
    @Final
    private WanderingTrader trader;

    @ModifyExpressionValue(
            method = { "canUse", "tick" },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/npc/wanderingtrader/WanderingTrader;"
                            + "getWanderTarget()Lnet/minecraft/core/BlockPos;"))
    private @Nullable BlockPos toroidal$wanderTargetThroughSeam(@Nullable BlockPos wanderTarget) {
        return wanderTarget == null ? null : SeamSteering.nearestCopy(this.trader, wanderTarget);
    }
}
