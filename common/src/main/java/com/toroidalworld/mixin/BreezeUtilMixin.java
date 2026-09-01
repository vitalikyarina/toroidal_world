package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.monster.breeze.Breeze;
import net.minecraft.world.entity.monster.breeze.BreezeUtil;
import net.minecraft.world.phys.Vec3;

@Mixin(BreezeUtil.class)
public class BreezeUtilMixin {
    @ModifyVariable(method = "hasLineOfSight", at = @At("HEAD"), argsOnly = true)
    private static Vec3 toroidal$sightTargetThroughSeam(Vec3 target, @Local(argsOnly = true) Breeze breeze) {
        return SeamAim.nearestTo(breeze, target);
    }
}
