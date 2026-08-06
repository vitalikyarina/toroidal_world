package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Ghast;

// A ghast does not turn through a look control: it writes its own yaw from a raw difference to its target. That is not
// only how it faces — the fireball leaves from a point four blocks along its view vector, so a yaw pointing the long
// way round the world also puts the shot's origin on the wrong side of the ghast.
@Mixin(Ghast.class)
public class GhastFacingAimMixin {
    @ModifyExpressionValue(
            method = "faceMovementDirection(Lnet/minecraft/world/entity/Mob;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D"))
    private static double toroidal$aimTargetX(double targetX, @Local(argsOnly = true) Mob ghast) {
        return SeamAim.nearX(ghast, targetX);
    }

    @ModifyExpressionValue(
            method = "faceMovementDirection(Lnet/minecraft/world/entity/Mob;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D"))
    private static double toroidal$aimTargetZ(double targetZ, @Local(argsOnly = true) Mob ghast) {
        return SeamAim.nearZ(ghast, targetZ);
    }
}
