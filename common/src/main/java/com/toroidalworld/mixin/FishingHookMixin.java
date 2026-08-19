package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

import net.minecraft.world.entity.projectile.FishingHook;

@Mixin(FishingHook.class)
public class FishingHookMixin {
    @ModifyExpressionValue(
            method = "pullEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double toroidal$pullTowardsOwnerX(double ownerX) {
        return SeamAim.nearX((FishingHook) (Object) this, ownerX);
    }

    @ModifyExpressionValue(
            method = "pullEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double toroidal$pullTowardsOwnerZ(double ownerZ) {
        return SeamAim.nearZ((FishingHook) (Object) this, ownerZ);
    }

    @ModifyExpressionValue(
            method = "retrieve",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getX()D", ordinal = 0))
    private double toroidal$throwLootTowardsOwnerX(double ownerX) {
        return SeamAim.nearX((FishingHook) (Object) this, ownerX);
    }

    @ModifyExpressionValue(
            method = "retrieve",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getZ()D", ordinal = 0))
    private double toroidal$throwLootTowardsOwnerZ(double ownerZ) {
        return SeamAim.nearZ((FishingHook) (Object) this, ownerZ);
    }
}
