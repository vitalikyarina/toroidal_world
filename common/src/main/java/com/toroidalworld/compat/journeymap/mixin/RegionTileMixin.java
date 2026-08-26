package com.toroidalworld.compat.journeymap.mixin;

import java.util.function.ToDoubleFunction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.Window;
import com.toroidalworld.compat.journeymap.JourneyMapFold;

import journeymap.api.v2.common.Context.UI;
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
    public abstract void render(GuiGraphicsExtractor graphics, Matrix3x2fStack pose, UI context,
            double pixelOffsetX, double pixelOffsetZ, float alpha, MapType mapType, RenderPipeline pipeline);

    @Shadow(remap = false)
    private int zoom;

    // Render-thread only, like the render call itself.
    @Unique
    private static boolean toroidal$drawingCopies;

    @Inject(method = "render", at = @At("TAIL"))
    private void toroidal$renderWrappedCopies(GuiGraphicsExtractor graphics, Matrix3x2fStack pose, UI context,
            double pixelOffsetX, double pixelOffsetZ, float alpha, MapType mapType, RenderPipeline pipeline,
            CallbackInfo ci) {
        if (toroidal$drawingCopies || context == UI.Webmap) {
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

        toroidal$drawingCopies = true;
        try {
            for (int lapX = -rangeX; lapX <= rangeX; lapX++) {
                for (int lapZ = -rangeZ; lapZ <= rangeZ; lapZ++) {
                    if (lapX == 0 && lapZ == 0) {
                        continue;
                    }

                    this.render(graphics, pose, context,
                            pixelOffsetX + lapX * periodX, pixelOffsetZ + lapZ * periodZ, alpha, mapType, pipeline);
                }
            }
        } finally {
            toroidal$drawingCopies = false;
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
