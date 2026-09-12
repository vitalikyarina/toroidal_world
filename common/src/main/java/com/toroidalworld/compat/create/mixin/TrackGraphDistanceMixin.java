package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.graph.TrackGraph;
import com.toroidalworld.InjectionTargets;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = TrackGraph.class, remap = false)
public class TrackGraphDistanceMixin {
    @Unique
    private static final String RANKED_METHOD = "distanceToLocationSqr";

    @WrapOperation(method = RANKED_METHOD, at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR))
    private double toroidal$rankNodeTheShortWayRound(Vec3 node, Vec3 location, Operation<Double> original,
            @Local(argsOnly = true) Level level) {
        return original.call(CreateSeamFold.nearestCopy(level, location, node), location);
    }
}
