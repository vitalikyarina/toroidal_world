package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.Train;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Train.class, remap = false)
public class TrainDistanceMixin {
    private static final String RANKED_METHOD = "distanceToLocationSqr";
    private static final String VEC3_DISTANCE_TO_SQR =
            "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D";

    @WrapOperation(method = RANKED_METHOD, at = @At(value = "INVOKE", target = VEC3_DISTANCE_TO_SQR))
    private double toroidal$rankAnchorTheShortWayRound(Vec3 anchor, Vec3 location, Operation<Double> original,
            @Local(argsOnly = true) Level level) {
        return original.call(CreateSeamFold.nearestCopy(level, location, anchor), location);
    }
}
