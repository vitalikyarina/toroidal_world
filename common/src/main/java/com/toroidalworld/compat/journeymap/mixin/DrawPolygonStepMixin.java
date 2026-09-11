package com.toroidalworld.compat.journeymap.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.journeymap.OverlayCopies;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.AbstractTexture;

import journeymap.api.v2.client.model.ShapeProperties;

@Mixin(targets = "journeymap.client.render.draw.DrawPolygonStep", remap = false)
public abstract class DrawPolygonStepMixin {
    @WrapOperation(method = "drawGeometry",
            at = @At(value = "INVOKE",
                    target = "Ljourneymap/client/render/draw/DrawUtil;drawPolygon(Lnet/minecraft/client/gui/GuiGraphics;"
                            + "Lnet/minecraft/client/renderer/MultiBufferSource;DDLjava/util/List;Ljava/util/List;"
                            + "Ljava/util/List;Lnet/minecraft/client/renderer/texture/AbstractTexture;"
                            + "Ljourneymap/api/v2/client/model/ShapeProperties;)V"))
    private void toroidal$polygonOnEveryCopy(GuiGraphics graphics, MultiBufferSource buffers, double xOffset,
            double yOffset, List<?> fillPoints, List<?> strokePoints, List<?> texturePoints, AbstractTexture texture,
            ShapeProperties properties, Operation<Void> original) {
        for (double[] copy : ((OverlayCopies) (Object) this).toroidal$copies()) {
            original.call(graphics, buffers, xOffset + copy[0], yOffset + copy[1], fillPoints, strokePoints,
                    texturePoints, texture, properties);
        }
    }
}
