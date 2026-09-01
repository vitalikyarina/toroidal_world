package com.toroidalworld.mixin;

import java.util.Comparator;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.bee.Bee;

@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeeLocateHiveGoal")
public class BeeLocateHiveGoalMixin {
    @Shadow(aliases = "this$0")
    @Final
    private Bee bee;

    @ModifyExpressionValue(
            method = "findNearbyHivesWithSpace",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/Comparator;comparingDouble(Ljava/util/function/ToDoubleFunction;)Ljava/util/Comparator;"))
    private Comparator<BlockPos> toroidal$rankHivesThroughSeam(Comparator<BlockPos> byRawDistance,
            @Local BlockPos beePos) {
        Bee rankingBee = this.bee;
        WorldFold transformer = ((TransformerSource) rankingBee).toroidal$wrappedTransformer();
        if (transformer == null) {
            return byRawDistance;
        }

        return Comparator.comparingDouble(hivePos -> SeamSteering.nearestCopy(rankingBee, hivePos).distSqr(beePos));
    }
}
