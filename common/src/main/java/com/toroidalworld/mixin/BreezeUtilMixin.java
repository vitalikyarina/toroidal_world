package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.breeze.BreezeUtil;
import net.minecraft.world.phys.Vec3;

// The breeze keeps its own copy of the sight check, outside ai/ and shaped as the general one was before it was folded:
// a raw distance refused past 50 blocks, and only then the ray. Across the seam the range gate alone refuses, so the ray
// never runs and neither of the two behaviours built on it ever starts — the long jump is gated entirely by this call.
//
// The target becomes its copy nearest the breeze, which is where it physically is, so the gate and the ray both measure
// the short way; block reads along the ray wrap on their way to a chunk exactly as they do for the general sight line.
// A target already on this side arrives unchanged and reads exactly as vanilla reads it.
@Mixin(BreezeUtil.class)
public class BreezeUtilMixin {
    @ModifyVariable(method = "hasLineOfSight", at = @At("HEAD"), argsOnly = true)
    private static Vec3 toroidal$sightTargetThroughSeam(Vec3 target, @Local(argsOnly = true) Breeze breeze) {
        return SeamAim.nearestTo(breeze, target);
    }
}
