package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.entity.SeamAim;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
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
// On this game version the two are asked in different shapes. Exposure still has a primitive to wrap — the entity is
// handed to it, so it can answer for any caller. The direction has none: vanilla builds it from three separate
// subtractions of the centre's fields, inline in explode(), and assembles a Vec3 only after the result has been
// normalized and scaled. There is nothing shared to wrap, so the two horizontal reads are corrected where they are
// made, each restated as the centre plus the short way to the entity — which is what the subtraction below it was
// always meant to produce. An explosion that does not cross the seam folds to what it already was.
@Mixin(Explosion.class)
public class ServerExplosionMixin {
    @Shadow
    @Final
    private double x;

    @Shadow
    @Final
    private double z;

    @ModifyVariable(
            method = "getSeenPercent(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/Entity;)F",
            at = @At("HEAD"), argsOnly = true)
    private static Vec3 toroidal$exposureCentreThroughSeam(Vec3 centre, @Local(argsOnly = true) Entity entity) {
        Vec3 folded = SeamAim.nearestTo(entity, centre);
        return folded;
    }

    @ModifyExpressionValue(
            method = "explode",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double toroidal$knockbackOriginX(double entityX, @Local Entity entity) {
        double folded = this.x + SeamAim.foldX(entity, entityX - this.x);
        return folded;
    }

    @ModifyExpressionValue(
            method = "explode",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double toroidal$knockbackOriginZ(double entityZ, @Local Entity entity) {
        double folded = this.z + SeamAim.foldZ(entity, entityZ - this.z);
        return folded;
    }
}
