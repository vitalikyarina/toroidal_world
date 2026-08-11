package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.predicate.SeamDistanceBounds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.advancements.critereon.DistancePredicate;
import net.minecraft.advancements.critereon.DistanceTrigger;
import net.minecraft.advancements.critereon.FallAfterExplosionTrigger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

// Two readers of a distance bound, one arithmetic (see SeamDistanceBounds): each hands DistancePredicate two absolute
// positions and the predicate subtracts them raw. A kill five blocks away through the seam then measures half a world,
// which awards adventure/sniper_duel and adventure/bullseye to someone standing next to their target; the same reading
// in the other direction makes the at-most bound of the lightning criteria unreachable.
//
// Written on the call rather than on the method it sits in, because both of these read the same position again through
// a LocationPredicate beforehand — that one asks the world about a place, which already folds, and it stays on the
// coordinates it was given.
//
// The third reader, the `distance` field of every EntityPredicate, is in EntityPredicateDistanceMixin: its matches has
// a player-flavoured overload that never touches DistancePredicate, so the wrap needs the exact descriptor and cannot
// share this one's plain method name.
@Mixin({DistanceTrigger.TriggerInstance.class, FallAfterExplosionTrigger.TriggerInstance.class})
public class DistanceBoundsMixin {
    @WrapOperation(
            method = "matches",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/advancements/critereon/DistancePredicate;matches(DDDDDD)Z"))
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
