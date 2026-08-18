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

// The stretch a carriage occupies, handed to the collision search as two absolute points. Its two ends are the leading
// and trailing travelling points, which across the seam answer from two frames — so the search would sweep the whole
// world between them and meet every train in it. The far end is brought to the copy nearest the near one, which is the
// carriage's own length again; which of the two is which depends on the direction of travel, so the near one is taken
// from the local the method has already filled rather than guessed.
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
