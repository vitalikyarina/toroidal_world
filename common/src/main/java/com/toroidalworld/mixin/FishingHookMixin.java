package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamAim;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;

@Mixin(FishingHook.class)
public class FishingHookMixin {
    @WrapOperation(
            method = "pullEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D"))
    private double toroidal$pullTowardsOwnerX(Entity target, Operation<Double> original) {
        return SeamAim.nearestTo((FishingHook) (Object) this, target.position()).x;
    }

    @WrapOperation(
            method = "pullEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D"))
    private double toroidal$pullTowardsOwnerZ(Entity target, Operation<Double> original) {
        return SeamAim.nearestTo((FishingHook) (Object) this, target.position()).z;
    }

    @WrapOperation(
            method = "retrieve",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getX()D", ordinal = 0))
    private double toroidal$throwLootTowardsOwnerX(Player target, Operation<Double> original) {
        return SeamAim.nearestTo((FishingHook) (Object) this, target.position()).x;
    }

    @WrapOperation(
            method = "retrieve",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getZ()D", ordinal = 0))
    private double toroidal$throwLootTowardsOwnerZ(Player target, Operation<Double> original) {
        return SeamAim.nearestTo((FishingHook) (Object) this, target.position()).z;
    }
}
