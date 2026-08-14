package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.journeymap.JourneyMapFold;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// The root of the long-way-round defect: a waypoint stores the coordinate the player had when it was created, and
// every JourneyMap renderer measures it against the player's current mirror coordinate with a plain subtraction.
// All of those reads funnel through the position getters folded here — the in-world beacon, the screen labels, the
// locator-bar angle and the distance cull inherit the fold without knowing it. Persistence is untouched: the codec
// serializes the stored WaypointPos directly, never through these getters.
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

    // Recomputed from the folded getter rather than the stored block position the vanilla body reads — the two
    // bodies agree exactly on an unwrapped level (getPosition is the block-centered position), so the guard only
    // spares the extra call there.
    @Inject(method = "distanceSquared", at = @At("HEAD"), cancellable = true)
    private void toroidal$foldDistanceSquared(Entity entity, CallbackInfoReturnable<Double> cir) {
        if (JourneyMapFold.active()) {
            cir.setReturnValue(entity.distanceToSqr(this.getPosition()));
        }
    }
}
