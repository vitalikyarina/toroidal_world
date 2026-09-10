package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.toroidalworld.compat.journeymap.JourneyMapFold;

@Mixin(targets = "journeymap.client.model.map.MapState", remap = false)
public abstract class MapStateMixin {
    @WrapMethod(method = "setZoom(I)Z")
    private boolean toroidal$refuseBelowZoomFloor(int zoom, Operation<Boolean> original) {
        return zoom >= JourneyMapFold.zoomFloor() && original.call(zoom);
    }
}
