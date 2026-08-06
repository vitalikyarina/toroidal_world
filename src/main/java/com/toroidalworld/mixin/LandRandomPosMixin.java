package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

// The ground-walking arm of the family (see SeamSteering). The shorter getPosAway is an overload that hands its work to
// the one taken here, so both reach the fold.
@Mixin(LandRandomPos.class)
public class LandRandomPosMixin {
    @ModifyVariable(method = "getPosTowards", at = @At("HEAD"), argsOnly = true)
    private static Vec3 toroidal$towardsThroughSeam(Vec3 towardsPos, @Local(argsOnly = true) PathfinderMob mob) {
        return SeamSteering.nearestCopy(mob, towardsPos);
    }

    @ModifyVariable(
            method = "getPosAway(Lnet/minecraft/world/entity/PathfinderMob;DDILnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;",
            at = @At("HEAD"), argsOnly = true)
    private static Vec3 toroidal$awayThroughSeam(Vec3 avoidPos, @Local(argsOnly = true) PathfinderMob mob) {
        return SeamSteering.nearestCopy(mob, avoidPos);
    }
}
