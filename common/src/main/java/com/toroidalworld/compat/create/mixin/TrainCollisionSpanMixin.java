package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.Train;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Train.class, remap = false)
public abstract class TrainCollisionSpanMixin {
    @ModifyExpressionValue(method = "collideWithOtherTrains",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/trains/entity/TravellingPoint;getPosition(Lcom/simibubi/create/content/trains/graph/TrackGraph;)Lnet/minecraft/world/phys/Vec3;",
                    ordinal = 1))
    private Vec3 toroidal$foldCarriageSpan(Vec3 farEnd, @Local(ordinal = 0) Vec3 nearEnd,
            @Local ResourceKey<Level> dimension) {
        return CreateTrackFold.nearestCopy(dimension, nearEnd, farEnd);
    }
}
