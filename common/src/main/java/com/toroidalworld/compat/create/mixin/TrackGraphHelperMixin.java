package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.trains.graph.TrackGraphHelper;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = TrackGraphHelper.class, remap = false)
public abstract class TrackGraphHelperMixin {
    @WrapOperation(method = "getGraphLocationAt",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;subtract(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 toroidal$foldWalkDelta(Vec3 target, Vec3 anchor, Operation<Vec3> original, Level level,
            BlockPos pos, AxisDirection targetDirection, Vec3 targetAxis) {
        return original.call(CreateSeamFold.nearestCopy(level, anchor, target), anchor);
    }

    @WrapOperation(method = "getGraphLocationAt",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/phys/Vec3;distanceTo(Lnet/minecraft/world/phys/Vec3;)D"))
    private static double toroidal$foldNodeDistance(Vec3 anchor, Vec3 target, Operation<Double> original, Level level,
            BlockPos pos, AxisDirection targetDirection, Vec3 targetAxis) {
        return original.call(anchor, CreateSeamFold.nearestCopy(level, anchor, target));
    }
}
