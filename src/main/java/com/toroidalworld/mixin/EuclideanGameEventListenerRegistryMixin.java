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
import net.minecraft.world.level.gameevent.EuclideanGameEventListenerRegistry;

// Whether a game-event listener is in range of an event is a distance test — a sculk sensor across the seam is a whole
// world from the sound and is dropped before anything else runs. Measured through the seam it is a step away.
@Mixin(EuclideanGameEventListenerRegistry.class)
public class EuclideanGameEventListenerRegistryMixin {
    @WrapOperation(
            method = "getPostableListenerPosition",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;distSqr(Lnet/minecraft/core/Vec3i;)D"))
    private static double toroidal$rangeThroughSeam(BlockPos listenerPos, Vec3i sourcePos, Operation<Double> original,
            @Local(argsOnly = true) ServerLevel level) {
        return SeamRange.sqr(level, listenerPos, sourcePos);
    }
}
