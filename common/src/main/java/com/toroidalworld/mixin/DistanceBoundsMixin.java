package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.predicate.SeamDistanceBounds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.advancements.predicates.DistancePredicate;
import net.minecraft.advancements.predicates.entity.DistanceToPlayerPredicate;
import net.minecraft.advancements.triggers.DistanceTrigger;
import net.minecraft.advancements.triggers.FallAfterExplosionTrigger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

@Mixin({
        DistanceTrigger.TriggerInstance.class,
        FallAfterExplosionTrigger.TriggerInstance.class,
        DistanceToPlayerPredicate.class})
public class DistanceBoundsMixin {
    @WrapOperation(
            method = "matches",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/advancements/predicates/DistancePredicate;matches(DDDDDD)Z"))
    private boolean toroidal$boundThroughSeam(DistancePredicate bounds,
            double referenceX, double referenceY, double referenceZ,
            double measuredX, double measuredY, double measuredZ,
            Operation<Boolean> original, @Local(argsOnly = true) ServerLevel level) {
        Vec3 folded = SeamDistanceBounds.nearestCopy(level,
                new Vec3(referenceX, referenceY, referenceZ),
                new Vec3(measuredX, measuredY, measuredZ));
        return original.call(bounds, referenceX, referenceY, referenceZ, folded.x, folded.y, folded.z);
    }
}
