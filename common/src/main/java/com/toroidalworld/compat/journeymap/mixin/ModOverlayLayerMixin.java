package com.toroidalworld.compat.journeymap.mixin;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.toroidalworld.compat.journeymap.OverlayCopies;

import journeymap.client.render.draw.OverlayDrawStep;

@Mixin(targets = "journeymap.client.ui.fullscreen.layer.ModOverlayLayer", remap = false)
public abstract class ModOverlayLayerMixin {
    @WrapOperation(method = "updateOverlayState",
            at = @At(value = "INVOKE", target = "Ljava/awt/geom/Rectangle2D$Double;contains(Ljava/awt/geom/Point2D;)Z"))
    private boolean toroidal$touchedOnAnyCopy(Rectangle2D.Double bounds, Point2D mouse, Operation<Boolean> original,
            @Local OverlayDrawStep step) {
        if (!(step instanceof OverlayCopies copies)) {
            return original.call(bounds, mouse);
        }

        for (double[] copy : copies.toroidal$copies()) {
            if (bounds.contains(mouse.getX() - copy[0], mouse.getY() - copy[1])) {
                return true;
            }
        }

        return false;
    }
}
