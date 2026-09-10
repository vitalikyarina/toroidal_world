package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.toroidalworld.compat.journeymap.JourneyMapFold;

import net.minecraft.world.phys.Vec3;

@Mixin(targets = "journeymap.client.waypoint.ClientWaypointImpl", remap = false)
public abstract class ClientWaypointImplMixin {
    @ModifyReturnValue(method = "getPosition", at = @At("RETURN"))
    private Vec3 toroidal$foldPosition(Vec3 original) {
        return JourneyMapFold.nearestToPlayer(original);
    }
}
