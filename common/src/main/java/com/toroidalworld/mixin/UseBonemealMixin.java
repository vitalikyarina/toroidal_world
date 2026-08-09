package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.ai.behavior.UseBonemeal;
import net.minecraft.world.entity.npc.Villager;

// The same shape as the harvest: the crop is picked from the cube around the farmer and then remembered for the eighty
// ticks the bonemealing lasts, so the seam only gets between them when the farmer crosses it mid-session. After that
// the gate reads a crop under its own feet as a world away and no bone meal is ever applied.
@Mixin(UseBonemeal.class)
public class UseBonemealMixin {
    @WrapOperation(
            method = "tick(Lnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/world/entity/npc/Villager;J)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private boolean toroidal$cropReachThroughSeam(BlockPos cropPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) Villager body) {
        return SeamRange.closerToCenterThan(body, cropPos, bodyPosition, distance);
    }
}
