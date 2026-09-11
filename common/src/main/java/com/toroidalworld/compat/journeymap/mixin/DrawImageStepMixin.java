package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.journeymap.OverlayCopies;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

@Mixin(targets = "journeymap.client.render.draw.DrawImageStep", remap = false)
public abstract class DrawImageStepMixin {
    @WrapOperation(method = "draw",
            at = @At(value = "INVOKE",
                    target = "Ljourneymap/client/render/draw/DrawUtil;drawTexture(Lnet/minecraft/client/gui/GuiGraphicsExtractor;"
                            + "Lnet/minecraft/resources/Identifier;IFDDDDDZZZ)V"))
    private void toroidal$imageOnEveryCopy(GuiGraphicsExtractor graphics, Identifier texture, int color, float alpha,
            double x, double y, double width, double height, double rotation, boolean flip, boolean blur, boolean clamp,
            Operation<Void> original) {
        for (double[] copy : ((OverlayCopies) (Object) this).toroidal$copies()) {
            original.call(graphics, texture, color, alpha, x + copy[0], y + copy[1], width, height, rotation, flip,
                    blur, clamp);
        }
    }
}
