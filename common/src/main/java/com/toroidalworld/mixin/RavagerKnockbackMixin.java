package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Ravager;

@Mixin(Ravager.class)
public class RavagerKnockbackMixin {
    @ModifyExpressionValue(
            method = "strongKnockback(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double toroidal$shoveTargetNearX(double targetX) {
        return SeamAim.nearX((Entity) (Object) this, targetX);
    }

    @ModifyExpressionValue(
            method = "strongKnockback(Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double toroidal$shoveTargetNearZ(double targetZ) {
        return SeamAim.nearZ((Entity) (Object) this, targetZ);
    }
}
