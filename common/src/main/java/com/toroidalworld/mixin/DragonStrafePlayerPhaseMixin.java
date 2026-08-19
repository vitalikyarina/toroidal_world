package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.boss.enderdragon.phases.DragonStrafePlayerPhase;

@Mixin(DragonStrafePlayerPhase.class)
public class DragonStrafePlayerPhaseMixin {
    @ModifyExpressionValue(
            method = "doServerTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D"))
    private double toroidal$aimTargetX(double targetX) {
        return SeamAim.nearX(((DragonPhaseAccessor) this).toroidal$dragon(), targetX);
    }

    @ModifyExpressionValue(
            method = "doServerTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D"))
    private double toroidal$aimTargetZ(double targetZ) {
        return SeamAim.nearZ(((DragonPhaseAccessor) this).toroidal$dragon(), targetZ);
    }
}
