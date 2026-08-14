package com.toroidalworld.compat.xaero.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.xaero.XaeroFold;

import net.minecraft.core.Direction;

// The root of the long-way-round distances: a waypoint stores the coordinate the player had when it was created,
// and every distance Xaero derives from it — the max-distance cull in both renderers, the in-world "123m" label,
// the waypoint-list readout, the hover pick, the one-off destination auto-remove — reads it through these
// dimension-scale getters and subtracts a camera or player position. Folding the getters to the copy nearest the
// camera makes all of that measure the short way without knowing it. Persistence and the edit GUI read the plain
// no-argument getters and stay untouched.
@Mixin(targets = "xaero.common.minimap.waypoints.Waypoint", remap = false)
public abstract class WaypointMixin {
    @Inject(method = "getX(D)I", at = @At("RETURN"), cancellable = true)
    private void toroidal$foldX(CallbackInfoReturnable<Integer> cir) {
        int folded = XaeroFold.nearestWaypointBlock(Direction.Axis.X, cir.getReturnValue());
        if (folded != cir.getReturnValue()) {
            cir.setReturnValue(folded);
        }
    }

    @Inject(method = "getZ(D)I", at = @At("RETURN"), cancellable = true)
    private void toroidal$foldZ(CallbackInfoReturnable<Integer> cir) {
        int folded = XaeroFold.nearestWaypointBlock(Direction.Axis.Z, cir.getReturnValue());
        if (folded != cir.getReturnValue()) {
            cir.setReturnValue(folded);
        }
    }
}
