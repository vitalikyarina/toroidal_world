package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.toroidalworld.compat.journeymap.OverlayCopies;

import net.minecraft.client.gui.GuiGraphics;

@Mixin(targets = {"journeymap.client.render.draw.DrawImageStep", "journeymap.client.render.draw.DrawMarkerStep"},
        remap = false)
public abstract class DrawQuadStepMixin {
    @WrapOperation(method = "draw",
            at = @At(value = "INVOKE",
                    target = "Ljourneymap/client/render/draw/DrawUtil;drawQuad(Lnet/minecraft/client/gui/GuiGraphics;"
                            + "Lcom/mojang/blaze3d/vertex/VertexConsumer;IFDDDDDDDDDZ)V"))
    private void toroidal$quadOnEveryCopy(GuiGraphics graphics, VertexConsumer vertices, int color, float alpha,
            double x, double y, double width, double height, double minU, double minV, double maxU, double maxV,
            double rotation, boolean flip, Operation<Void> original) {
        for (double[] copy : ((OverlayCopies) (Object) this).toroidal$copies()) {
            original.call(graphics, vertices, color, alpha, x + copy[0], y + copy[1], width, height, minU, minV,
                    maxU, maxV, rotation, flip);
        }
    }
}
