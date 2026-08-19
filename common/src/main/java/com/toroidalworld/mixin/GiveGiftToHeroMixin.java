package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.ai.behavior.GiveGiftToHero;
import net.minecraft.world.entity.npc.villager.Villager;

@Mixin(GiveGiftToHero.class)
public class GiveGiftToHeroMixin {
    @WrapOperation(
            method = "isWithinThrowingDistance",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"))
    private boolean toroidal$throwingReachThroughSeam(BlockPos villagerPos, Vec3i playerPos, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) Villager villager) {
        return SeamRange.closerThan(villager, villagerPos, playerPos, distance);
    }
}
