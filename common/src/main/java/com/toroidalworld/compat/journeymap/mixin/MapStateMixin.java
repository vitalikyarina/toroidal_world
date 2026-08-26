package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.toroidalworld.compat.journeymap.JourneyMapFold;

@Mixin(targets = "journeymap.client.model.map.MapState", remap = false)
public abstract class MapStateMixin {
    @Inject(method = "setZoom(I)Z", at = @At("HEAD"), cancellable = true)
    private void toroidal$refuseBelowZoomFloor(int zoom, CallbackInfoReturnable<Boolean> cir) {
        if (zoom < JourneyMapFold.zoomFloor()) {
            cir.setReturnValue(false);
        }
    }
}
