package com.toroidalworld.compat.xaero.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.xaero.XaeroFold;

import net.minecraft.core.Direction;

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
