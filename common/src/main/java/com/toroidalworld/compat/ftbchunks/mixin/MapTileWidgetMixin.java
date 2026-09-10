package com.toroidalworld.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold.TileBlit;

import net.minecraft.client.gui.GuiGraphics;

import dev.ftb.mods.ftbchunks.client.gui.MapTileWidget;
import dev.ftb.mods.ftbchunks.client.gui.RegionMapPanel;
import dev.ftb.mods.ftbchunks.client.map.MapRegion;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.Widget;

@Mixin(value = MapTileWidget.class, remap = false)
public abstract class MapTileWidgetMixin {
    @Shadow
    @Final
    public MapRegion region;

    // Tiles go under the seam lines RegionMapPanel draws between Panel's two layers; the icons stay above both.
    @Inject(method = "<init>", at = @At("RETURN"))
    private void toroidal$drawUnderTheSeamLines(RegionMapPanel panel, MapRegion region, CallbackInfo ci) {
        ((Widget) (Object) this).setDrawLayer(Widget.DrawLayer.BACKGROUND);
    }

    // The tile paints only its part inside the world: the copies already lie beside it, and its black margin would
    // paint over them.
    @WrapOperation(method = "draw",
            at = @At(value = "INVOKE",
                    target = "Ldev/ftb/mods/ftblibrary/ui/GuiHelper;drawTexturedRect(Lnet/minecraft/client/gui/GuiGraphics;"
                            + "IIIILdev/ftb/mods/ftblibrary/icon/Color4I;FFFF)V"))
    private void toroidal$blitWorldPartOnly(GuiGraphics graphics, int x, int y, int w, int h, Color4I color,
            float u0, float v0, float u1, float v1, Operation<Void> original) {
        TileBlit blit = FtbChunksFold.worldPartOf(this.region.pos.x(), this.region.pos.z(), x, y, w, h);
        if (blit == null) {
            return;
        }

        original.call(graphics, blit.x(), blit.y(), blit.width(), blit.height(), color,
                blit.u0(), blit.v0(), blit.u1(), blit.v1());
    }
}
