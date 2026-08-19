package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.Position;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.warden.WardenSpawnTracker;
import net.minecraft.world.phys.Vec3;

@Mixin(WardenSpawnTracker.class)
public class WardenSpawnTrackerMixin {
    @WrapOperation(
            method = "*",
            require = 1,
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"))
    private static boolean toroidal$warningRangeThroughSeam(Vec3 playerPosition, Position shriekerOrigin,
            double distance, Operation<Boolean> original, @Local(argsOnly = true) ServerPlayer player) {
        return SeamRange.closerThan(player, playerPosition, shriekerOrigin, distance);
    }
}
