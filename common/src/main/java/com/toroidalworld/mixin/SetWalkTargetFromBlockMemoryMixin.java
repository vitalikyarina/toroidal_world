package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromBlockMemory;
import net.minecraft.world.entity.npc.villager.Villager;

@Mixin(SetWalkTargetFromBlockMemory.class)
public class SetWalkTargetFromBlockMemoryMixin {
    @WrapOperation(
            method = "lambda$create$2",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distManhattan(Lnet/minecraft/core/Vec3i;)I"))
    private static int toroidal$memoryDistanceThroughSeam(BlockPos from, Vec3i to, Operation<Integer> original,
            @Local(argsOnly = true) Villager body) {
        return SeamRange.manhattan(body, from, to);
    }
}
