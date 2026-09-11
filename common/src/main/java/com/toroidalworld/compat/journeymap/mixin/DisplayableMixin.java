package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.toroidalworld.compat.journeymap.SingleCopyOverlay;

@Mixin(targets = "journeymap.api.v2.client.display.Displayable", remap = false)
public abstract class DisplayableMixin implements SingleCopyOverlay {
    @Unique
    private boolean toroidal$drawsOnce;

    @Override
    public void toroidal$drawOnce() {
        toroidal$drawsOnce = true;
    }

    @Override
    public boolean toroidal$drawsOnce() {
        return toroidal$drawsOnce;
    }
}
