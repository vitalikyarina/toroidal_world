package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.LongJumpUtil;
import net.minecraft.world.phys.Vec3;

@Mixin(LongJumpUtil.class)
public class LongJumpUtilMixin {
    @ModifyVariable(method = "calculateJumpVectorForAngle", at = @At("HEAD"), argsOnly = true)
    private static Vec3 toroidal$jumpTargetThroughSeam(Vec3 targetPos, @Local(argsOnly = true) Mob body) {
        return SeamAim.nearestTo(body, targetPos);
    }
}
