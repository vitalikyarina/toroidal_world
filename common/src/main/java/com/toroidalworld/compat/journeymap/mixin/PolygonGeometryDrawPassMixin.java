package com.toroidalworld.compat.journeymap.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.toroidalworld.compat.journeymap.JourneyMapSeamPass;

import journeymap.client.render.draw.DrawPolygonStep;
import journeymap.client.render.map.Renderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix3x2fStack;

@Mixin(targets = "journeymap.client.render.map.PolygonGeometryDrawPass", remap = false)
public abstract class PolygonGeometryDrawPassMixin {
    @Inject(method = "draw", at = @At("HEAD"))
    private void toroidal$drawSeamLines(GuiGraphicsExtractor graphics, Matrix3x2fStack pose,
            List<? extends DrawPolygonStep> polygonSteps, double xOffset, double yOffset, Renderer renderer,
            double fontScale, double rotation, CallbackInfo ci) {
        if (renderer instanceof JourneyMapSeamPass seamPass) {
            seamPass.toroidal$drawSeams(graphics, pose, xOffset, yOffset);
        }
    }
}
