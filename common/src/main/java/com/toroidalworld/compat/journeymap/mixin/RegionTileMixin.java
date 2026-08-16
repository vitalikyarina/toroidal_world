package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Coerce;

import com.toroidalworld.compat.journeymap.JourneyMapFold;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import journeymap.api.v2.client.display.Context.UI;
import journeymap.client.model.map.MapType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;
import org.joml.Matrix3x2fStack;

// Glues the map together across the seam: after a tile draws itself, it is drawn again at every world-width period
// that still lands in the viewport, so the torus reads as the endlessly repeating ground it is — on the minimap at
// the seam and on the fullscreen map alike. This is also what swallows the fullscreen pan jump: the folded center
// moves by exactly one period, and a picture periodic in that period looks identical. The tile renders by
// (this.x + pixelOffset), so a copy is one more call with shifted offsets — no matrix work, no touching the caller's
// loop (which sits in a lambda). The quad collector belongs to that caller and is drawn once after every region has
// filled it, so the copies leave in the same batch as the tile they repeat. Webmap tiles are left alone: their
// offsets are web-tile space, not the on-screen grid.
//
// Wrapping the method rather than injecting at its tail, because the collector this game version threads through
// render is package-private to JourneyMap: it cannot be named from here, and @Coerce carries it as an opaque Object
// only through an injector's own parameters — a @Shadow of render would have to spell the type out. Calling through
// the Operation also means the copies re-enter the original body rather than this handler, so no re-entry flag is
// needed to keep a copy from spawning copies of its own.
@Mixin(targets = "journeymap.client.render.map.RegionTile", remap = false)
public abstract class RegionTileMixin {
    @Shadow(remap = false)
    private int zoom;

    @WrapMethod(method = "render")
    private void toroidal$renderWrappedCopies(GuiGraphicsExtractor graphics, @Coerce Object quads,
            Matrix3x2fStack pose, UI context, double pixelOffsetX, double pixelOffsetZ, float alpha, MapType mapType,
            int shaderIndex, Operation<Void> original) {
        original.call(graphics, quads, pose, context, pixelOffsetX, pixelOffsetZ, alpha, mapType, shaderIndex);
        if (context == UI.Webmap) {
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

        for (int lapX = -rangeX; lapX <= rangeX; lapX++) {
            for (int lapZ = -rangeZ; lapZ <= rangeZ; lapZ++) {
                if (lapX == 0 && lapZ == 0) {
                    continue;
                }

                original.call(graphics, quads, pose, context,
                        pixelOffsetX + lapX * periodX, pixelOffsetZ + lapZ * periodZ, alpha, mapType, shaderIndex);
            }
        }
    }
}
