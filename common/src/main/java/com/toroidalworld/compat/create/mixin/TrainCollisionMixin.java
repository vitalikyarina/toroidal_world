package com.toroidalworld.compat.create.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.trains.entity.Train;
import com.toroidalworld.compat.create.CreateTrackFold;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Train.class, remap = false)
public abstract class TrainCollisionMixin {
    private static final String COLLIDING_METHOD = "findCollidingTrain";
    private static final String TRAVELLING_POINT_POSITION =
            "Lcom/simibubi/create/content/trains/entity/TravellingPoint;"
                    + "getPosition(Lcom/simibubi/create/content/trains/graph/TrackGraph;)"
                    + "Lnet/minecraft/world/phys/Vec3;";
    private static final String VEC3_ADD =
            "Lnet/minecraft/world/phys/Vec3;add(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;";

    private static final String OTHER_START_LOCAL = "start2";
    private static final int SPAN_END_ARGUMENT = 1;
    private static final int SPAN_START_ARGUMENT = 0;

    @ModifyVariable(method = COLLIDING_METHOD, at = @At("HEAD"), argsOnly = true, ordinal = SPAN_END_ARGUMENT)
    private Vec3 toroidal$foldOwnSpan(Vec3 spanEnd, @Local(argsOnly = true) Level level,
            @Local(argsOnly = true, ordinal = SPAN_START_ARGUMENT) Vec3 spanStart) {
        return CreateTrackFold.nearestCopy(level, spanStart, spanEnd);
    }

    @ModifyExpressionValue(method = COLLIDING_METHOD,
            at = @At(value = "INVOKE", target = TRAVELLING_POINT_POSITION, ordinal = 0))
    private Vec3 toroidal$foldOtherStart(Vec3 otherStart, @Local(argsOnly = true) Level level,
            @Local(argsOnly = true, ordinal = SPAN_START_ARGUMENT) Vec3 spanStart) {
        return CreateTrackFold.nearestCopy(level, spanStart, otherStart);
    }

    @ModifyExpressionValue(method = COLLIDING_METHOD,
            at = @At(value = "INVOKE", target = TRAVELLING_POINT_POSITION, ordinal = 1))
    private Vec3 toroidal$foldOtherEnd(Vec3 otherEnd, @Local(argsOnly = true) Level level,
            @Local(name = OTHER_START_LOCAL) Vec3 otherStart) {
        return CreateTrackFold.nearestCopy(level, otherStart, otherEnd);
    }

    @ModifyExpressionValue(method = COLLIDING_METHOD, at = @At(value = "INVOKE", target = VEC3_ADD))
    private Vec3 toroidal$wrapCollisionPoint(Vec3 point, @Local(argsOnly = true) Level level) {
        return level instanceof ServerLevel serverLevel ? CreateTrackFold.wrap(serverLevel, point) : point;
    }
}
