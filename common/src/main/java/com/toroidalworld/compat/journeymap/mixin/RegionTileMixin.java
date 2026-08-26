package com.toroidalworld.compat.journeymap.mixin;

import java.util.function.ToDoubleFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Coerce;

import com.mojang.blaze3d.platform.Window;
import com.toroidalworld.compat.journeymap.JourneyMapFold;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import journeymap.api.v2.client.display.Context.UI;
import journeymap.client.model.map.MapType;
import journeymap.client.ui.UIManager;
import journeymap.client.ui.minimap.DisplayVars;
import net.minecraft.client.Minecraft;
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
        if (loopedAxes == 0) {
            return;
        }

        double periodX = JourneyMapFold.worldPixelPeriod(Direction.Axis.X, this.zoom);
        double periodZ = JourneyMapFold.worldPixelPeriod(Direction.Axis.Z, this.zoom);
        Window window = Minecraft.getInstance().getWindow();
        int viewportX = toroidal$viewportPixels(context, window.getWidth(), DisplayVars::getMinimapWidth);
        int viewportZ = toroidal$viewportPixels(context, window.getHeight(), DisplayVars::getMinimapHeight);
        int tiles = JourneyMapFold.tilesWithContent(this.zoom, viewportX, viewportZ);
        int rangeX = JourneyMapFold.copyRange(loopedAxes, tiles, periodX, viewportX);
        int rangeZ = JourneyMapFold.copyRange(loopedAxes, tiles, periodZ, viewportZ);
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
    private static int toroidal$viewportPixels(UI context, int windowPixels, ToDoubleFunction<DisplayVars> minimapSide) {
        if (context != UI.Minimap) {
            return windowPixels;
        }

        DisplayVars displayVars = UIManager.INSTANCE.getMiniMap().getDisplayVars();
        return displayVars == null ? windowPixels : (int) Math.ceil(minimapSide.applyAsDouble(displayVars));
    }
}
