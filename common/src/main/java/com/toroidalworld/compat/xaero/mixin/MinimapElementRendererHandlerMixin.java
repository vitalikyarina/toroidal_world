package com.toroidalworld.compat.xaero.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.toroidalworld.compat.xaero.XaeroFold;

import net.minecraft.core.Direction;

// Every minimap element — waypoint icons on the minimap, the clamped edge arrows, the in-world icons and labels —
// is positioned by one of three handlers, and all three subtract the render position from an element coordinate
// this shared base method hands them. Folding the coordinate here, at the one dispatch point, takes each element
// to the copy nearest the camera before any handler measures it: an element just across the seam draws beside the
// player instead of a world away. Only the unscaled dispatch is folded (ordinal 0) — the scaled branch mixes two
// dimensions' coordinate spaces, where a nearest-copy against the camera would fold in the wrong space; an element
// viewed through a foreign-dimension scale stays a stated limitation.
@Mixin(targets = "xaero.hud.minimap.element.render.MinimapElementRendererHandler", remap = false)
public abstract class MinimapElementRendererHandlerMixin {
    @ModifyArgs(
            method = "transformAndRenderForRenderer(Ljava/lang/Object;Lxaero/hud/minimap/element/render/MinimapElementRenderer;Ljava/lang/Object;IDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/hud/minimap/element/render/MinimapElementRendererHandler;transformAndRenderForRenderer(Ljava/lang/Object;DDDLxaero/hud/minimap/element/render/MinimapElementRenderer;Ljava/lang/Object;IDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
                    ordinal = 0))
    private void toroidal$foldElementCoords(Args args) {
        double elementX = args.get(1);
        double elementZ = args.get(3);
        args.set(1, XaeroFold.nearestElementCoord(Direction.Axis.X, elementX));
        args.set(3, XaeroFold.nearestElementCoord(Direction.Axis.Z, elementZ));
    }
}
