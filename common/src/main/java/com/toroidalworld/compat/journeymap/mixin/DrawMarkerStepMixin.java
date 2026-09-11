package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.journeymap.OverlayCopies;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

@Mixin(targets = "journeymap.client.render.draw.DrawMarkerStep", remap = false)
public abstract class DrawMarkerStepMixin {
    @WrapOperation(method = "draw",
            at = @At(value = "INVOKE",
                    target = "Ljourneymap/client/render/draw/DrawUtil;drawOnMapImage(Lnet/minecraft/client/gui/GuiGraphicsExtractor;"
                            + "Lnet/minecraft/resources/Identifier;IFDDDDFFFFDZ)V"))
    private void toroidal$markerOnEveryCopy(GuiGraphicsExtractor graphics, Identifier texture, int color, float alpha,
            double x, double y, double width, double height, float minU, float maxU, float minV, float maxV,
            double rotation, boolean blur, Operation<Void> original) {
        for (double[] copy : ((OverlayCopies) (Object) this).toroidal$copies()) {
            original.call(graphics, texture, color, alpha, x + copy[0], y + copy[1], width, height, minU, maxU, minV,
                    maxV, rotation, blur);
        }
    }
}
