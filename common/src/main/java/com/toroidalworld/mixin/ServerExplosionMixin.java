package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.entity.SeamAim;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;

// A blast reaches an entity across the seam already: the search box is cut at the bounds (LevelMixin) and the range gate
// runs on Entity.distanceToSqr, which measures the short way (EntityMixin). Everything the blast then measures against
// that entity is a raw difference from the centre, and there are two of them.
//
// Exposure is the ray drawn from each sample point on the entity's body to the centre. Taken raw across the seam it runs
// the long way round through the whole world, is stopped by the first thing in it, and comes back as nothing seen at
// all — which is not merely weak but a floor: the damage curve at zero exposure collapses to its constant term, one
// half-heart, and the knockback, which is exposure times everything else, to zero. So the single missing fold accounts
// for both halves of what a player sees. It also costs: that ray is walked once per sample point, up to a few hundred
// per entity, each of them the width of the world.
//
// Knockback direction is the difference itself, and it points away from the wrong side even once exposure is restored.
//
// Both are the same absent fact — where the centre is, seen from this entity — asked once of the exposure primitive,
// which holds the entity it is measuring and can therefore answer for any caller, and once of the difference. An
// explosion that does not cross the seam folds to what it already was, so ordinary blast damage and knockback, and the
// exact number of blocks the rays walk, are unchanged.
@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {
    // Folded once on entry rather than at each sample point: the samples all sit on the one body, and there are up to a
    // few hundred of them.
    @ModifyVariable(
            method = "getSeenPercent(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;)F",
            at = @At("HEAD"), argsOnly = true)
    private static Vec3 toroidal$exposureCentreThroughSeam(Vec3 centre, @Local(argsOnly = true) Entity entity) {
        return SeamAim.nearestTo(entity, centre);
    }

    // The method carries a different signature on each loader: vanilla hurts entities from hurtEntities() with no
    // arguments, NeoForge patches the live path into hurtEntities(List) and keeps a deprecated no-argument delegate
    // whose body holds no subtract. Both names are listed and require = 1 accepts the one body that has the call.
    @ModifyExpressionValue(
            method = { "hurtEntities()V", "hurtEntities(Ljava/util/List;)V" },
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"),
            require = 1)
    private Vec3 toroidal$knockbackDirectionThroughSeam(Vec3 delta, @Local Entity entity) {
        return SeamAim.foldDelta(entity, delta);
    }
}
