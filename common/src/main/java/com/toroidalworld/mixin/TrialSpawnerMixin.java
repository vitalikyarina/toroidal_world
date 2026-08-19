package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;

@Mixin(TrialSpawner.class)
public class TrialSpawnerMixin {
    @WrapOperation(
            method = "shouldMobBeUntracked",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"))
    private static double toroidal$mobLeashThroughSeam(BlockPos mobPos, Vec3i spawnerPos, Operation<Double> original,
            @Local(argsOnly = true) ServerLevel level) {
        return SeamRange.sqr(level, mobPos, spawnerPos);
    }
}
