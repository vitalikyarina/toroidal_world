package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.compat.journeymap.JourneyMapFold;

import journeymap.api.v2.common.Context.UI;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;
import org.joml.Matrix3x2fStack;

// Glues the map together across the seam: after a tile draws itself, it is drawn again at every world-width period
// that still lands in the viewport, so the torus reads as the endlessly repeating ground it is — on the minimap at
// the seam and on the fullscreen map alike. This is also what swallows the fullscreen pan jump: the folded center
// moves by exactly one period, and a picture periodic in that period looks identical. The tile renders by
// (this.x + pixelOffset), so a copy is one re-invocation with shifted offsets — no matrix work, no touching the
// caller's loop (which sits in a lambda). Webmap tiles are left alone: their offsets are web-tile space, not the
// on-screen grid.
@Mixin(targets = "journeymap.client.render.map.RegionTile", remap = false)
public abstract class RegionTileMixin {
    @Shadow(remap = false)
    public abstract void render(GuiGraphicsExtractor graphics, Matrix3x2fStack pose, UI context,
            double pixelOffsetX, double pixelOffsetZ, float alpha);

    @Shadow(remap = false)
    private int zoom;

    // Render-thread only, like the render call itself.
    @Unique
    private static boolean toroidal$drawingCopies;

    @Inject(method = "render", at = @At("TAIL"))
    private void toroidal$renderWrappedCopies(GuiGraphicsExtractor graphics, Matrix3x2fStack pose, UI context,
            double pixelOffsetX, double pixelOffsetZ, float alpha, CallbackInfo ci) {
        if (toroidal$drawingCopies || context == UI.Webmap) {
            return;
        }

        double periodX = JourneyMapFold.worldPixelPeriod(Direction.Axis.X, this.zoom);
        double periodZ = JourneyMapFold.worldPixelPeriod(Direction.Axis.Z, this.zoom);
        int rangeX = JourneyMapFold.copyRange(periodX, graphics.guiWidth());
        int rangeZ = JourneyMapFold.copyRange(periodZ, graphics.guiHeight());
        // The fullscreen map caps at one copy per side (at most 3x3) — the canonical world in the middle, its glued
        // neighbours around it; zoomed far out the ground past those copies is left empty rather than repeated to
        // the horizon.
        if (context == UI.Fullscreen) {
            rangeX = Math.min(rangeX, 1);
            rangeZ = Math.min(rangeZ, 1);
        }

        if (rangeX == 0 && rangeZ == 0) {
            return;
        }

        toroidal$drawingCopies = true;
        try {
            for (int lapX = -rangeX; lapX <= rangeX; lapX++) {
                for (int lapZ = -rangeZ; lapZ <= rangeZ; lapZ++) {
                    if (lapX == 0 && lapZ == 0) {
                        continue;
                    }

                    this.render(graphics, pose, context,
                            pixelOffsetX + lapX * periodX, pixelOffsetZ + lapZ * periodZ, alpha);
                }
            }
        } finally {
            toroidal$drawingCopies = false;
        }
    }
}
