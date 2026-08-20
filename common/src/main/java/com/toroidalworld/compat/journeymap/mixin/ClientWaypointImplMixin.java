package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.journeymap.JourneyMapFold;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

@Mixin(targets = "journeymap.client.waypoint.ClientWaypointImpl", remap = false)
public abstract class ClientWaypointImplMixin {
    @Shadow(remap = false)
    public abstract Vec3 getPosition();

    @Inject(method = "getPosition", at = @At("RETURN"), cancellable = true)
    private void toroidal$foldPosition(CallbackInfoReturnable<Vec3> cir) {
        Vec3 folded = JourneyMapFold.nearestToPlayer(cir.getReturnValue());
        if (folded != cir.getReturnValue()) {
            cir.setReturnValue(folded);
        }
    }

    @Inject(method = "positionFromPlayer", at = @At("RETURN"), cancellable = true)
    private void toroidal$foldFromPlayer(CallbackInfoReturnable<Vec3> cir) {
        Vec3 folded = JourneyMapFold.nearestToPlayer(cir.getReturnValue());
        if (folded != cir.getReturnValue()) {
            cir.setReturnValue(folded);
        }
    }

    @Inject(method = "distanceSquared", at = @At("HEAD"), cancellable = true)
    private void toroidal$foldDistanceSquared(Entity entity, CallbackInfoReturnable<Double> cir) {
        if (JourneyMapFold.active()) {
            cir.setReturnValue(entity.distanceToSqr(this.getPosition()));
        }
    }
}
