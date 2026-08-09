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
import net.minecraft.world.entity.npc.Villager;

// Everything up to the throw measures the hero as an entity and so already reads through the seam: the villager sees
// them, walks to them, turns to face them. Only the last question — are we within five blocks — is asked of two block
// positions, and there the seam returns. The villager stands beside its hero holding a gift it will never throw, and
// the gift cooldown it is waiting on never restarts.
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
