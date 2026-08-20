package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.equine.Llama;

@Mixin(Llama.class)
public class LlamaSpitAimMixin {
    @ModifyExpressionValue(
            method = "spit(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getX()D"))
    private double toroidal$aimTargetX(double targetX) {
        return SeamAim.nearX((Entity) (Object) this, targetX);
    }

    @ModifyExpressionValue(
            method = "spit(Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getZ()D"))
    private double toroidal$aimTargetZ(double targetZ) {
        return SeamAim.nearZ((Entity) (Object) this, targetZ);
    }
}
