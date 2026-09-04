package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.InjectionTargets;
import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.ai.behavior.GiveGiftToHero;
import net.minecraft.world.entity.npc.Villager;

@Mixin(GiveGiftToHero.class)
public class GiveGiftToHeroMixin {
    @WrapOperation(
            method = "isWithinThrowingDistance",
            at = @At(value = "INVOKE", target = InjectionTargets.BLOCK_POS_CLOSER_THAN))
    private boolean toroidal$throwingReachThroughSeam(BlockPos villagerPos, Vec3i playerPos, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) Villager villager) {
        return SeamRange.closerThan(villager, villagerPos, playerPos, distance);
    }
}
