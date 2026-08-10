package com.toroidalworld.mixin;

import java.util.Comparator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    // From the constructor, not shadowed off this$0 — see BeeEnterHiveGoalMixin: the outer reference is javac's, not
    // any mapping set's, so a remapping loader has nothing to resolve it to.
    @Unique
    private Bee toroidal$bee;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/animal/Bee;)V", at = @At("TAIL"))
    private void toroidal$captureBee(Bee bee, CallbackInfo ci) {
        this.toroidal$bee = bee;
    }

    @ModifyExpressionValue(
            method = "findNearbyHivesWithSpace",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/Comparator;comparingDouble(Ljava/util/function/ToDoubleFunction;)Ljava/util/Comparator;"))
    private Comparator<BlockPos> toroidal$rankHivesThroughSeam(Comparator<BlockPos> byRawDistance,
            @Local BlockPos beePos) {
        Bee rankingBee = this.toroidal$bee;
        WorldLoopTransformer transformer = ((TransformerSource) rankingBee).toroidal$wrappedTransformer();
        if (transformer == null) {
            return byRawDistance;
        }

        return Comparator.comparingDouble(hivePos -> SeamSteering.nearestCopy(rankingBee, hivePos).distSqr(beePos));
    }
}
