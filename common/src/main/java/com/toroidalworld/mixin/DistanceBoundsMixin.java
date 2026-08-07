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

// Three readers of a distance bound, one arithmetic (see SeamDistanceBounds): each hands DistancePredicate two
// absolute positions and the predicate subtracts them raw. A kill five blocks away through the seam then measures
// half a world, which awards adventure/sniper_duel and adventure/bullseye to someone standing next to their target;
// the same reading in the other direction makes the at-most bound of the lightning criteria unreachable.
//
// Written on the call rather than on the method it sits in, because two of these three read the same position again
// through a LocationPredicate beforehand — that one asks the world about a place, which already folds, and it stays on
// the coordinates it was given.
//
// DistanceToPlayerPredicate is here for the entity predicate rather than for a criterion of its own: it is the
// `distance` field of every EntityPredicate, so it carries the bound of a loot table or a predicate file as well.
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
