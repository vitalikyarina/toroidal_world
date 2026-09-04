package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.predicate.SeamDistanceBounds;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.advancements.predicates.DistancePredicate;
import net.minecraft.advancements.triggers.LevitationTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

@Mixin(LevitationTrigger.TriggerInstance.class)
public class LevitationBoundsMixin {
    @WrapOperation(
            method = "matches",
            at = @At(value = "INVOKE",
                    target = InjectionTargets.DISTANCE_PREDICATE_MATCHES))
    private boolean toroidal$boundThroughSeam(DistancePredicate bounds,
            double referenceX, double referenceY, double referenceZ,
            double measuredX, double measuredY, double measuredZ,
            Operation<Boolean> original, @Local(argsOnly = true) ServerPlayer player) {
        Vec3 folded = SeamDistanceBounds.nearestCopy(player.level(),
                new Vec3(referenceX, referenceY, referenceZ),
                new Vec3(measuredX, measuredY, measuredZ));
        return original.call(bounds, referenceX, referenceY, referenceZ, folded.x, folded.y, folded.z);
    }
}
