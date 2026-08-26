package com.toroidalworld.compat.journeymap.mixin;

import java.util.function.ToDoubleFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Coerce;

import com.toroidalworld.compat.journeymap.JourneyMapFold;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import journeymap.api.v2.client.display.Context.UI;
import journeymap.client.model.map.MapType;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.DisplayVars;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Direction;
import org.joml.Matrix3x2fStack;

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

        int loopedAxes = JourneyMapFold.loopedAxes();
        double periodX = JourneyMapFold.worldPixelPeriod(Direction.Axis.X, this.zoom);
        double periodZ = JourneyMapFold.worldPixelPeriod(Direction.Axis.Z, this.zoom);
        int viewportX = toroidal$viewportPixels(context, graphics.guiWidth(), DisplayVars::getMinimapWidth);
        int viewportZ = toroidal$viewportPixels(context, graphics.guiHeight(), DisplayVars::getMinimapHeight);
        int rangeX = JourneyMapFold.copyRange(loopedAxes, periodX, viewportX);
        int rangeZ = JourneyMapFold.copyRange(loopedAxes, periodZ, viewportZ);
        if (context == UI.Fullscreen) {
            rangeX = Math.min(rangeX, 1);
            rangeZ = Math.min(rangeZ, 1);
        }

        JourneyMapFold.logTileCopies(context.name(), this.zoom, loopedAxes, periodX, periodZ,
                viewportX, viewportZ, graphics.guiWidth(), graphics.guiHeight());
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

    @Unique
    private static int toroidal$viewportPixels(UI context, int guiPixels, ToDoubleFunction<DisplayVars> minimapSide) {
        if (context != UI.Minimap) {
            return guiPixels;
        }

        DisplayVars displayVars = UIManager.INSTANCE.getMiniMap().getDisplayVars();
        return displayVars == null ? guiPixels : (int) Math.ceil(minimapSide.applyAsDouble(displayVars));
    }
}
