package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.journeymap.JourneyMapFold;

import net.minecraft.world.phys.Vec3;

// The root of the long-way-round defect: a waypoint stores the coordinate the player had when it was created, and
// every JourneyMap renderer measures it against the player's current mirror coordinate with a plain subtraction.
// On this game version every in-world read funnels through this one getter — the beacon, the screen labels and the
// distance math all take their Vec3 from getPosition and subtract from it directly — so folding it here is the
// whole fix. The map-side draw steps read the raw stored ints instead, which the pixel fold in MapRendererMixin
// already takes to the copy nearest the map center. Persistence is untouched: the codec serializes the stored
// WaypointPos directly, never through this getter.
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
