package com.toroidalworld.compat.journeymap.mixin;

import java.awt.geom.Rectangle2D;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "journeymap.client.render.map.MapRenderer", remap = false)
public interface MapRendererAccessor {
    @Accessor("screenBounds")
    Rectangle2D.Double toroidal$screenBounds();
}
