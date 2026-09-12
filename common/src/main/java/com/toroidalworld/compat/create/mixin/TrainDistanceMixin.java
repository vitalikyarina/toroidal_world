package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.Carriage;
import com.simibubi.create.content.trains.entity.Train;
import com.toroidalworld.InjectionTargets;
import com.toroidalworld.compat.create.CarriageEntityFrame;
import com.toroidalworld.compat.create.CreateSeamFold;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Train.class, remap = false)
public class TrainDistanceMixin {
    @Unique
    private static final String RANKED_METHOD = "distanceToLocationSqr";

    @WrapOperation(method = RANKED_METHOD, at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR))
    private double toroidal$rankAnchorTheShortWayRound(Vec3 anchor, Vec3 location, Operation<Double> original,
            @Local(argsOnly = true) Level level) {
        return original.call(CreateSeamFold.nearestCopy(level, location, anchor), location);
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = InjectionTargets.VEC3_DISTANCE_TO_SQR))
    private double toroidal$spanCarriagesTheShortWayRound(Vec3 leading, Vec3 trailing, Operation<Double> original,
            @Local(ordinal = 0) Carriage.DimensionalCarriageEntity dimensional) {
        return original.call(leading, CreateSeamFold.nearestCopy(
                ((CarriageEntityFrame) dimensional).toroidal$carriageDimension(), leading, trailing));
    }
}
