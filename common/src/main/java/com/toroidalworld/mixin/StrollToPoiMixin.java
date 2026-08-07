package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.toroidalworld.entity.SeamRange;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.StrollToPoi;

// A villager only strolls to a remembered place while it is still near enough to count as belonging there. The tether
// is a raw difference against a position written down in the world, and the mob asking is wherever the wrap funnel last
// left it — so across the seam it reads a villager standing beside its own meeting point as a world out, the behaviour
// refuses to run, and the walk target is never set.
//
// The gate is restated on the distance through the seam rather than the remembered place being moved: what the memory
// holds stays the position the POI actually occupies, which is what every later lookup on it needs.
@Mixin(StrollToPoi.class)
public class StrollToPoiMixin {
    @WrapOperation(
            method = "lambda$create$2",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/BlockPos;closerToCenterThan(Lnet/minecraft/core/Position;D)Z"))
    private static boolean toroidal$poiTetherThroughSeam(BlockPos poiPos, Position bodyPosition, double distance,
            Operation<Boolean> original, @Local(argsOnly = true) PathfinderMob body) {
        return SeamRange.closerToCenterThan(body, poiPos, bodyPosition, distance);
    }
}
