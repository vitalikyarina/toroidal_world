package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.predicate.SeamDistanceBounds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.advancements.critereon.DistancePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

// The third reader of a distance bound (see DistanceBoundsMixin): the `distance` field of every EntityPredicate, so it
// carries the bound of a loot table or a predicate file as well. Kept apart from the trigger pair because matches()
// has a player-flavoured overload that never touches DistancePredicate — the wrap needs the exact descriptor.
@Mixin(EntityPredicate.class)
public class EntityPredicateDistanceMixin {
    @WrapOperation(
            method = "matches(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;"
                    + "Lnet/minecraft/world/entity/Entity;)Z",
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
