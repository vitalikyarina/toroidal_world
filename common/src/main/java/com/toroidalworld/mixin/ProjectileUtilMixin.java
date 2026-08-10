package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.accessors.TransformerSource;
import com.toroidalworld.core.WorldLoopTransformer;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Everything a shot has to do already passes through the seam — the arrow flies across it, the block clip reads the far
// side through the Level chunk gate, and the search box that gathers candidates is cut at the bounds. Only the last step
// is blind: the flight segment is clipped against each candidate's raw hitbox, and a target standing past the boundary
// carries coordinates from the far edge of the world. The two never meet, so the clip misses and the arrow, trident,
// potion or fireball passes through a mob it visibly went into.
//
// The segment is one tick of flight — three blocks at full draw — so the strip where this happens is narrow, and it only
// fails one way round: the same shot fired back across the seam lands, because then the target is the one on this side.
//
// Each candidate's box becomes the copy nearest the ray start and vanilla's own arithmetic runs on that. The reference
// is the ray start rather than each target's own position: every candidate is then answered in one frame, which is what
// keeps the nearest-hit comparison between them meaningful and the returned hit location in the frame the projectile
// itself is moving through. A target on this side folds to itself, so an ordinary shot is unchanged.
//
// Wrapping the box where it is fetched covers all four readings vanilla takes of it — the clip, the pick-from-inside
// test, the centre the surface clip aims at, and the second fetch that clip runs on — with one choke point per method,
// and every one of them lands on the same copy.
@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {
    @WrapOperation(
            method = {
                    "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
                    "getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private static AABB toroidal$candidateBoxThroughSeam(Entity candidate, Operation<AABB> original,
            @Local(argsOnly = true, ordinal = 0) Vec3 from) {
        AABB box = original.call(candidate);
        WorldLoopTransformer transformer = ((TransformerSource) candidate).toroidal$wrappedTransformer();
        return transformer == null ? box : transformer.foldBoxToward(from, box);
    }
}
