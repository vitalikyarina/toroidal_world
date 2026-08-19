package com.toroidalworld.compat.xaero.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.toroidalworld.compat.xaero.XaeroFold;

import net.minecraft.core.Direction;

@Mixin(targets = "xaero.hud.minimap.element.render.MinimapElementRendererHandler", remap = false)
public abstract class MinimapElementRendererHandlerMixin {
    @ModifyArgs(
            method = "transformAndRenderForRenderer(Ljava/lang/Object;Lxaero/hud/minimap/element/render/MinimapElementRenderer;Ljava/lang/Object;IDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/hud/minimap/element/render/MinimapElementRendererHandler;transformAndRenderForRenderer(Ljava/lang/Object;DDDLxaero/hud/minimap/element/render/MinimapElementRenderer;Ljava/lang/Object;IDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
                    ordinal = 0))
    private void toroidal$foldElementCoords(Args args) {
        double elementX = args.get(1);
        double elementZ = args.get(3);
        args.set(1, XaeroFold.nearestElementCoord(Direction.Axis.X, elementX));
        args.set(3, XaeroFold.nearestElementCoord(Direction.Axis.Z, elementZ));
    }
}
