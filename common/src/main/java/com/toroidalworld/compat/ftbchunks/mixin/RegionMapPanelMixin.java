package com.toroidalworld.compat.ftbchunks.mixin;

import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold.SeamView;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold.TileBlit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

import dev.ftb.mods.ftbchunks.client.gui.LargeMapScreen;
import dev.ftb.mods.ftbchunks.client.gui.MapTileWidget;
import dev.ftb.mods.ftbchunks.client.gui.RegionMapPanel;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import dev.ftb.mods.ftblibrary.ui.Theme;
import dev.ftb.mods.ftblibrary.ui.Widget;

@Mixin(value = RegionMapPanel.class, remap = false)
public abstract class RegionMapPanelMixin {
    // MapTileWidget.draw switches to linear filtering once a tile is narrower than 512 window pixels.
    private static final double LINEAR_FILTER_BELOW_PIXELS = 512.0;
    private static final int REGION_BLOCKS = 512;
    private static final int SEAM_LINE_ARGB = 0xCCFFFFFF;
    private static final int SEAM_LINE_WIDTH = 1;

    @Shadow
    @Final
    LargeMapScreen largeMap;

    @Shadow
    int regionMinX;

    @Shadow
    int regionMinZ;

    @Unique
    private @Nullable SeamView toroidal$seamView;

    // A copy is the same tile blitted again at a lap's offset — never a second pass through Panel.draw, which drags
    // the icons, their pose changes and a fresh texture-id round trip along with it.
    @WrapOperation(method = "draw",
            at = @At(value = "INVOKE",
                    target = "Ldev/ftb/mods/ftblibrary/ui/Panel;draw(Lnet/minecraft/client/gui/GuiGraphics;"
                            + "Ldev/ftb/mods/ftblibrary/ui/Theme;IIII)V"))
    private void toroidal$drawWrappedCopies(RegionMapPanel panel, GuiGraphics graphics, Theme theme, int x, int y,
            int w, int h, Operation<Void> original) {
        int loopedAxes = FtbChunksFold.loopedAxes();
        if (loopedAxes == 0) {
            toroidal$seamView = null;
            original.call(panel, graphics, theme, x, y, w, h);
            return;
        }

        int tilePixels = this.largeMap.getRegionTileSize();
        double periodX = FtbChunksFold.worldPixelPeriod(Direction.Axis.X, tilePixels);
        double periodZ = FtbChunksFold.worldPixelPeriod(Direction.Axis.Z, tilePixels);

        // The laps are the ones whose copy falls inside the view in block terms — the canonical square can sit at
        // the edge of the scrolled canvas or past it, so a fixed reach around it leaves the far side bare.
        panel.setOffset(true);
        double pixelsPerBlock = tilePixels / (double) REGION_BLOCKS;
        int originX = panel.getX() - this.regionMinX * tilePixels;
        int originY = panel.getY() - this.regionMinZ * tilePixels;
        int[] lapsX = FtbChunksFold.copies(Direction.Axis.X).laps(
                Mth.floor((x - originX) / pixelsPerBlock), Mth.ceil((x + w - originX) / pixelsPerBlock));
        int[] lapsZ = FtbChunksFold.copies(Direction.Axis.Z).laps(
                Mth.floor((y - originY) / pixelsPerBlock), Mth.ceil((y + h - originY) / pixelsPerBlock));
        int reachX = 0;
        int reachZ = 0;

        // Copies go under the canonical pass: that pass draws the icons, and an icon straddling the seam has to
        // stay on top of the copy beside it.
        boolean scissor = panel.getOnlyRenderWidgetsInside();
        if (scissor) {
            GuiHelper.pushScissor(panel.getWindow(), x, y, w, h);
        }

        for (int lapX : lapsX) {
            for (int lapZ : lapsZ) {
                if (lapX == 0 && lapZ == 0) {
                    continue;
                }

                reachX = Math.max(reachX, Math.abs(lapX));
                reachZ = Math.max(reachZ, Math.abs(lapZ));
                toroidal$blitCopy(panel, graphics, x, y, w, h,
                        (int) Math.round(lapX * periodX), (int) Math.round(lapZ * periodZ));
            }
        }

        if (scissor) {
            GuiHelper.popScissor(panel.getWindow());
        }

        panel.setOffset(false);
        FtbChunksFold.recordLargeMapCopyRange(reachX, reachZ);

        toroidal$seamView = new SeamView(originX, originY, tilePixels, x, y, w, h);
        original.call(panel, graphics, theme, x, y, w, h);
    }

    // Panel.draw calls this between its BACKGROUND and FOREGROUND layers, under the panel's scissor: the tiles are
    // moved to BACKGROUND (MapTileWidgetMixin), so the lines land over the terrain and under every icon.
    public void drawOffsetBackground(GuiGraphics graphics, Theme theme, int x, int y, int w, int h) {
        SeamView view = toroidal$seamView;
        if (view == null) {
            return;
        }

        for (int seamX : FtbChunksFold.seamPixels(Direction.Axis.X, view.originX(), view.tilePixels(), view.x(),
                view.x() + view.width())) {
            graphics.fill(seamX, view.y(), seamX + SEAM_LINE_WIDTH, view.y() + view.height(), SEAM_LINE_ARGB);
        }

        for (int seamZ : FtbChunksFold.seamPixels(Direction.Axis.Z, view.originY(), view.tilePixels(), view.y(),
                view.y() + view.height())) {
            graphics.fill(view.x(), seamZ, view.x() + view.width(), seamZ + SEAM_LINE_WIDTH, SEAM_LINE_ARGB);
        }
    }

    private static void toroidal$blitCopy(RegionMapPanel panel, GuiGraphics graphics, int viewX, int viewY,
            int viewWidth, int viewHeight, int offsetX, int offsetY) {
        for (Widget widget : panel.getWidgets()) {
            if (!(widget instanceof MapTileWidget tile)) {
                continue;
            }

            // The id call is what uploads a pending image and sets the loaded flag, so it goes before the check —
            // the order MapTileWidget.draw keeps.
            int id = tile.region.getRenderedMapImageTextureId();
            if (!tile.region.isMapImageLoaded()) {
                continue;
            }

            TileBlit blit = FtbChunksFold.worldPartOf(tile.region.pos.x(), tile.region.pos.z(),
                    tile.getX() + offsetX, tile.getY() + offsetY, tile.width, tile.height);
            if (blit == null || blit.x() + blit.width() <= viewX || blit.x() >= viewX + viewWidth
                    || blit.y() + blit.height() <= viewY || blit.y() >= viewY + viewHeight) {
                continue;
            }

            RenderSystem.bindTextureForSetup(id);
            int filter = tile.width * Minecraft.getInstance().getWindow().getGuiScale() < LINEAR_FILTER_BELOW_PIXELS
                    ? GL11.GL_LINEAR
                    : GL11.GL_NEAREST;
            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, filter);
            RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, filter);
            RenderSystem.setShaderTexture(0, id);
            GuiHelper.drawTexturedRect(graphics, blit.x(), blit.y(), blit.width(), blit.height(), Color4I.WHITE,
                    blit.u0(), blit.v0(), blit.u1(), blit.v1());
        }
    }

    @ModifyExpressionValue(method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;floor(D)I", ordinal = 0))
    private int toroidal$foldPickX(int blockX) {
        return FtbChunksFold.foldBlock(Direction.Axis.X, blockX);
    }

    @ModifyExpressionValue(method = "draw",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;floor(D)I", ordinal = 1))
    private int toroidal$foldPickZ(int blockZ) {
        return FtbChunksFold.foldBlock(Direction.Axis.Z, blockZ);
    }
}
