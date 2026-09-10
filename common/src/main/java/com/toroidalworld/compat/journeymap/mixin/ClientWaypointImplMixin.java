package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.compat.journeymap.JourneyMapFold;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "journeymap.client.waypoint.ClientWaypointImpl", remap = false)
public abstract class ClientWaypointImplMixin {
    @Shadow(remap = false)
    public abstract Vec3 getPosition();

    @ModifyReturnValue(method = "getPosition", at = @At("RETURN"))
    private Vec3 toroidal$foldPosition(Vec3 original) {
        return JourneyMapFold.nearestToPlayer(original);
    }

    @ModifyReturnValue(method = "positionFromPlayer", at = @At("RETURN"))
    private Vec3 toroidal$foldFromPlayer(Vec3 original) {
        return JourneyMapFold.nearestToPlayer(original);
    }

    @WrapMethod(method = "distanceSquared")
    private double toroidal$foldDistanceSquared(Entity entity, Operation<Double> original) {
        return JourneyMapFold.active() ? entity.distanceToSqr(this.getPosition()) : original.call(entity);
    }
}
