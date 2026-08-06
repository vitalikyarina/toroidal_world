package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.phys.Vec3;

// A lead measures its length by plain distance, so a mob a step away across the seam read as a whole world off: the
// lead snapped the moment its holder crossed, and a fence on the far side refused the knot outright. One primitive
// carries all of it — the break check, the attach check and the elastic threshold — so it is fixed once, here.
@Mixin(Leashable.class)
public interface LeashableMixin {
    @WrapMethod(method = "leashDistanceTo")
    private double toroidal$leashDistanceThroughSeam(Entity entity, Operation<Double> original) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(entity.level());
        if (transformer == null) {
            return original.call(entity);
        }

        Vec3 from = ((Entity) this).getBoundingBox().getCenter();
        Vec3 to = entity.getBoundingBox().getCenter();
        return Math.sqrt(transformer.coords.sqrDistToBounds(from.x, from.y, from.z, to.x, to.y, to.z));
    }

    // The elastic pull is a spring toward the holder's absolute position; across the seam the raw vector aims the long
    // way round, with a world of magnitude. The holder is read at its representation nearest the leashed entity, so
    // the spring pulls the short way the lead visually hangs.
    @WrapOperation(
            method = "computeElasticInteraction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;position()Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1))
    private static Vec3 toroidal$holderPositionThroughSeam(Entity leashHolder, Operation<Vec3> original,
            @Local(argsOnly = true, ordinal = 0) Entity entity) {
        Vec3 position = original.call(leashHolder);
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(leashHolder.level());
        if (transformer == null) {
            return position;
        }

        return transformer.vectors.nearestCopy(entity.position(), position);
    }
}
