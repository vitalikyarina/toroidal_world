package com.toroidalworld.compat.sable.mixin;

import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.sable.SableTrackingRange;

import dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem;

import net.minecraft.server.level.ServerLevel;

@Mixin(value = SubLevelTrackingSystem.class, remap = false)
public class SubLevelTrackingSystemMixin {
    @Shadow
    @Final
    private ServerLevel level;

    @WrapOperation(
            method = "shouldLoad",
            at = @At(value = "INVOKE", target = "Lorg/joml/Vector3dc;distanceSquared(DDD)D"))
    private double toroidal$distanceTheShortWayRound(Vector3dc pose, double x, double y, double z, Operation<Double> original) {
        return SableTrackingRange.sqrDistance(this.level, pose, x, y, z, original);
    }
}
