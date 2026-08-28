package com.toroidalworld.compat.sable.mixin;

import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.sable.SableSeamDistance;

import dev.ryanhcode.sable.ActiveSableCompanion;

import net.minecraft.world.level.Level;

@Mixin(value = ActiveSableCompanion.class, remap = false)
public abstract class ActiveSableCompanionMixin {
    @WrapOperation(
            method = {
                    "distanceSquaredWithSubLevels(Lnet/minecraft/world/level/Level;Lorg/joml/Vector3dc;Lorg/joml/Vector3dc;)D",
                    "distanceSquaredWithSubLevels(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Position;Lnet/minecraft/core/Position;)D",
                    "distanceSquaredWithSubLevels(Lnet/minecraft/world/level/Level;Lorg/joml/Vector3dc;DDD)D",
                    "distanceSquaredWithSubLevels(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Position;DDD)D",
                    "distanceSquaredWithSubLevels(Lnet/minecraft/world/level/Level;DDDDDD)D"},
            at = @At(value = "INVOKE", target = "Lorg/joml/Vector3dc;distanceSquared(Lorg/joml/Vector3dc;)D"))
    private double toroidal$sqrDistanceThroughSeam(Vector3dc from, Vector3dc to, Operation<Double> original,
            @Local(argsOnly = true) Level level) {
        return SableSeamDistance.sqr(level, from, to, original);
    }

    @WrapOperation(
            method = {
                    "rectilinearDistanceWithSubLevels(Lnet/minecraft/world/level/Level;Lorg/joml/Vector3dc;Lorg/joml/Vector3dc;)D",
                    "rectilinearDistanceWithSubLevels(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Position;Lnet/minecraft/core/Position;)D",
                    "rectilinearDistanceWithSubLevels(Lnet/minecraft/world/level/Level;Lorg/joml/Vector3dc;DDD)D",
                    "rectilinearDistanceWithSubLevels(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Position;DDD)D",
                    "rectilinearDistanceWithSubLevels(Lnet/minecraft/world/level/Level;DDDDDD)D"},
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ryanhcode/sable/ActiveSableCompanion;rectilinearDistance(Lorg/joml/Vector3dc;Lorg/joml/Vector3dc;)D"))
    private double toroidal$rectilinearDistanceThroughSeam(Vector3dc from, Vector3dc to, Operation<Double> original,
            @Local(argsOnly = true) Level level) {
        return SableSeamDistance.rectilinear(level, from, to, original);
    }
}
