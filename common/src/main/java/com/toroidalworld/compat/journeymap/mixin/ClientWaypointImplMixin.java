package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.journeymap.JourneyMapFold;

import net.minecraft.world.phys.Vec3;

@Mixin(targets = "journeymap.client.waypoint.ClientWaypointImpl", remap = false)
public abstract class ClientWaypointImplMixin {
    @Inject(method = "getPosition", at = @At("RETURN"), cancellable = true)
    private void toroidal$foldPosition(CallbackInfoReturnable<Vec3> cir) {
        Vec3 folded = JourneyMapFold.nearestToPlayer(cir.getReturnValue());
        if (folded != cir.getReturnValue()) {
            cir.setReturnValue(folded);
        }
    }
}
