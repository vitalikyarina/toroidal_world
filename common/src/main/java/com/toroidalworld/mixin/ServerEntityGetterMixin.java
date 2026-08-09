package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.probe.ReshapeProbe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Which players a mob can see is asked of a box drawn around the mob, and every other entity query built on a box goes
// through Level.getEntities, which cuts a box reaching past the bounds into the pieces it physically covers. This one
// does not: it walks the player list itself and tests each one with a raw containment, so the box it uses is the one
// that runs off the edge of the world into ground nobody occupies.
//
// A player standing across the seam is therefore invisible to it, however close. The phantom's target scan is one of
// only two callers — which is why a phantom will not take a player a few blocks away over the boundary, and stays
// circling an anchor it has no reason to keep — and the iron golem defending its village is the other.
//
// The box is moved to its copy nearest the player being tested, and vanilla's own containment decides on that. A player
// on this side leaves the box where it was, so an ordinary scan reads exactly as it read before.
@Mixin(EntityGetter.class)
public interface ServerEntityGetterMixin {
    @WrapOperation(
            method = "getNearbyPlayers",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;contains(DDD)Z"))
    private static boolean toroidal$nearbyPlayerThroughSeam(AABB box, double x, double y, double z,
            Operation<Boolean> original, @Local(argsOnly = true) LivingEntity source) {
        WorldLoopTransformer transformer = ((TransformerSource) source).toroidal$wrappedTransformer();
        if (transformer == null) {
            ReshapeProbe.unwrapped(source.level().dimension(), ReshapeProbe.NEARBY_PLAYER);
            return original.call(box, x, y, z);
        }

        AABB folded = transformer.foldBoxToward(new Vec3(x, y, z), box);
        ReshapeProbe.fold(source.level().dimension(), ReshapeProbe.NEARBY_PLAYER,
                box.minX, box.minZ, folded.minX, folded.minZ);
        return original.call(folded, x, y, z);
    }
}
