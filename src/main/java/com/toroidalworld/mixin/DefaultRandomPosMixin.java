package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

// The land-and-water default of the family (see SeamSteering): both directed entry points subtract absolute positions,
// and the argument they subtract is read for nothing else.
@Mixin(DefaultRandomPos.class)
public class DefaultRandomPosMixin {
    @ModifyVariable(method = "getPosTowards", at = @At("HEAD"), argsOnly = true)
    private static Vec3 toroidal$towardsThroughSeam(Vec3 towardsPos, @Local(argsOnly = true) PathfinderMob mob) {
        return SeamSteering.nearestCopy(mob, towardsPos);
    }

    @ModifyVariable(method = "getPosAway", at = @At("HEAD"), argsOnly = true)
    private static Vec3 toroidal$awayThroughSeam(Vec3 avoidPos, @Local(argsOnly = true) PathfinderMob mob) {
        return SeamSteering.nearestCopy(mob, avoidPos);
    }
}
