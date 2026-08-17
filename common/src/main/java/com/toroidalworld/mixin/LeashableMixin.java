package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;

// A lead measures its length by plain distance, so a mob a step away across the seam read as a whole world off: the
// lead snapped the moment its holder crossed, and a fence on the far side refused the knot outright. Every one of those
// readings is Entity.distanceTo, which is already folded, so the length itself needs nothing here.
//
// What is left is the elastic pull — a spring toward the holder's absolute position. Across the seam the raw difference
// aims the long way round, with a world of magnitude, so the lead drags its mob away from the holder it visibly hangs
// from. Each horizontal coordinate of the holder is read at the copy nearest the leashed entity; the vertical read has
// no seam, and the distance the spring divides by arrives folded already, so the two agree.
//
// The pull is taken at this default method rather than the private static it delegates to, because that static is the
// implementation a mod replaces wholesale — Sable overwrites it for its sublevels — and mixin refuses to inject into a
// method another mixin of equal priority has merged, which fails the whole config at bootstrap. This method is the one
// vanilla leaves to be overridden, and Boat's own override, folded separately, is the proof of it. A holder on this
// side folds to itself, and the pull is then vanilla's own.
@Mixin(Leashable.class)
public interface LeashableMixin {
    @WrapMethod(method = "elasticRangeLeashBehaviour")
    private void toroidal$elasticPullThroughSeam(Entity leashHolder, float leashDistance, Operation<Void> original) {
        Entity leashed = (Entity) this;
        double holderX = SeamAim.nearX(leashed, leashHolder.getX());
        double holderZ = SeamAim.nearZ(leashed, leashHolder.getZ());
        if (holderX == leashHolder.getX() && holderZ == leashHolder.getZ()) {
            original.call(leashHolder, leashDistance);
            return;
        }

        double pullX = (holderX - leashed.getX()) / leashDistance;
        double pullY = (leashHolder.getY() - leashed.getY()) / leashDistance;
        double pullZ = (holderZ - leashed.getZ()) / leashDistance;
        leashed.setDeltaMovement(
                leashed.getDeltaMovement()
                        .add(Math.copySign(pullX * pullX * 0.4, pullX), Math.copySign(pullY * pullY * 0.4, pullY),
                                Math.copySign(pullZ * pullZ * 0.4, pullZ)));
    }
}
