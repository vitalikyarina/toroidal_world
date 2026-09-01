package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.ai.behavior.warden.SonicBoom;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.Vec3;

@Mixin(SonicBoom.class)
public class SonicBoomAimMixin {
    @ModifyExpressionValue(
            method = "lambda$tick$2",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getEyePosition()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 toroidal$boomTargetThroughSeam(Vec3 eyePosition, @Local(argsOnly = true) Warden body) {
        return SeamAim.nearestTo(body, eyePosition);
    }
}
