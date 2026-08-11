package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;

// A lead measures its length by plain distance, so a mob a step away across the seam read as a whole world off: the
// lead snapped the moment its holder crossed, and a fence on the far side refused the knot outright. Every one of those
// readings is Entity.distanceTo, which is already folded, so the length itself needs nothing here.
//
// What is left is the elastic pull — a spring toward the holder's absolute position. Across the seam the raw difference
// aims the long way round, with a world of magnitude, so the lead drags its mob away from the holder it visibly hangs
// from. Each horizontal coordinate of the holder is read at the copy nearest the leashed entity, which leaves the
// vertical read and the whole of vanilla's arithmetic untouched; the distance the spring divides by arrives folded
// already, so the two agree. A holder on this side is read as itself.
@Mixin(Leashable.class)
public interface LeashableMixin {
    @ModifyExpressionValue(
            method = "legacyElasticRangeLeashBehaviour",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D", ordinal = 0))
    private static double toroidal$holderXThroughSeam(double holderX,
            @Local(argsOnly = true, ordinal = 0) Entity leashed) {
        return SeamAim.nearX(leashed, holderX);
    }

    @ModifyExpressionValue(
            method = "legacyElasticRangeLeashBehaviour",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D", ordinal = 0))
    private static double toroidal$holderZThroughSeam(double holderZ,
            @Local(argsOnly = true, ordinal = 0) Entity leashed) {
        return SeamAim.nearZ(leashed, holderZ);
    }
}
