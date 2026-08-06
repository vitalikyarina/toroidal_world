package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamSteering;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.AirRandomPos;
import net.minecraft.world.phys.Vec3;

// The flying arm of the family (see SeamSteering): a bee heading for a hive or a flower across the seam. The undirected
// air walkers next door — HoverRandomPos and AirAndWaterRandomPos — are steered by the mob's own view vector rather than
// a difference of positions, so they never see the seam and are left alone.
@Mixin(AirRandomPos.class)
public class AirRandomPosMixin {
    @ModifyVariable(method = "getPosTowards", at = @At("HEAD"), argsOnly = true)
    private static Vec3 toroidal$towardsThroughSeam(Vec3 towardsPos, @Local(argsOnly = true) PathfinderMob mob) {
        return SeamSteering.nearestCopy(mob, towardsPos);
    }
}
