package com.toroidalworld.mixin;

import java.util.function.ToDoubleFunction;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.bee.Bee;

@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeeLocateHiveGoal")
public class BeeLocateHiveGoalMixin {
    @Shadow(aliases = "this$0")
    @Final
    private Bee bee;

    @ModifyArg(
            method = "findNearbyHivesWithSpace",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/Comparator;comparingDouble(Ljava/util/function/ToDoubleFunction;)Ljava/util/Comparator;"),
            index = 0)
    private ToDoubleFunction<BlockPos> toroidal$rankHivesThroughSeam(ToDoubleFunction<BlockPos> byRawDistance,
            @Local BlockPos beePos) {
        WorldFold transformer = ((TransformerSource) this.bee).toroidal$wrappedTransformer();
        if (transformer == null) {
            return byRawDistance;
        }

        return hivePos -> byRawDistance.applyAsDouble(transformer.nearestCopy(beePos, hivePos));
    }
}
