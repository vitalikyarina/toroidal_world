package com.toroidalworld.mixin;

import java.util.function.ToDoubleFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldFold;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;

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

    @ModifyArg(
            method = "findNearbyHivesWithSpace",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/Comparator;comparingDouble(Ljava/util/function/ToDoubleFunction;)Ljava/util/Comparator;"),
            index = 0)
    private ToDoubleFunction<BlockPos> toroidal$rankHivesThroughSeam(ToDoubleFunction<BlockPos> byRawDistance,
            @Local BlockPos beePos) {
        WorldFold transformer = ((TransformerSource) this.toroidal$bee).toroidal$wrappedTransformer();
        if (transformer == null) {
            return byRawDistance;
        }

        return hivePos -> byRawDistance.applyAsDouble(transformer.nearestCopy(beePos, hivePos));
    }
}
