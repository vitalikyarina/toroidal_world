package com.toroidalworld.mixin;

import java.util.Comparator;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;

// Choosing a hive, rather than returning to one: the search itself already reaches across the seam, so a hive on the
// far side does come back as a candidate — but the candidates are then ranked by a raw distance, which puts a hive
// three blocks away through the boundary behind one nineteen blocks away on this side, and the bee takes the first.
//
// The comparator is replaced whole rather than the distance inside it: the ranking expression is a lambda, which
// compiles to a method of its own that an injector scoped to this method would not see at all.
@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeeLocateHiveGoal")
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
        WorldLoopTransformer transformer = ((TransformerSource) rankingBee).toroidal$wrappedTransformer();
        if (transformer == null) {
            return byRawDistance;
        }

        return Comparator.comparingDouble(hivePos -> SeamSteering.nearestCopy(rankingBee, hivePos).distSqr(beePos));
    }
}
