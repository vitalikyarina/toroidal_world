package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.phys.Vec3;

@Mixin(Leashable.class)
public interface LeashableMixin {
    @WrapMethod(method = "leashDistanceTo")
    private double toroidal$leashDistanceThroughSeam(Entity entity, Operation<Double> original) {
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(entity.level());
        if (transformer == null) {
            return original.call(entity);
        }

        Vec3 from = ((Entity) this).getBoundingBox().getCenter();
        Vec3 to = entity.getBoundingBox().getCenter();
        return Math.sqrt(transformer.sqrDistance(from.x, from.y, from.z, to.x, to.y, to.z));
    }

    @WrapOperation(
            method = "computeElasticInteraction",
            at = @At(
                    value = "INVOKE",
                    target = InjectionTargets.ENTITY_POSITION,
                    ordinal = 1))
    private static Vec3 toroidal$holderPositionThroughSeam(Entity leashHolder, Operation<Vec3> original,
            @Local(argsOnly = true, ordinal = 0) Entity entity) {
        Vec3 position = original.call(leashHolder);
        WorldFold transformer = WorldLoopAttachments.wrappedTransformerOf(leashHolder.level());
        if (transformer == null) {
            return position;
        }

        return transformer.nearestCopy(entity.position(), position);
    }
}
