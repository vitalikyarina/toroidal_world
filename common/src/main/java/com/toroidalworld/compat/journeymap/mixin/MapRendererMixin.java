package com.toroidalworld.compat.journeymap.mixin;

import java.awt.geom.Point2D;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.toroidalworld.compat.journeymap.JourneyMapFold;
import com.toroidalworld.compat.journeymap.JourneyMapSeamPass;

import journeymap.api.v2.client.util.UIState;
import journeymap.api.v2.client.display.Context;
import journeymap.client.render.JMRenderTypes;
import journeymap.client.render.draw.DrawUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.joml.Matrix3x2fStack;

@Mixin(targets = "journeymap.client.render.map.MapRenderer", remap = false)
public abstract class MapRendererMixin implements JourneyMapSeamPass {
    @Shadow(remap = false)
    protected double centerBlockX;

    @Shadow(remap = false)
    protected double centerBlockZ;

    @Shadow(remap = false)
    protected int zoom;

    @Shadow(remap = false)
    public abstract void clear();

    @Shadow(remap = false)
    public abstract UIState getUIState();

    @Shadow(remap = false)
    public abstract Point2D.Double getBlockPixelInGrid(BlockPos pos);

    @Unique
    private static final int SEAM_ARGB = 0x59FFFFFF;

    // Render-thread only, like every caller of these methods.
    @Unique
    private static boolean toroidal$anchorPass;

    @Unique
    private ResourceKey<Level> toroidal$lastLevelDimension;

    // JourneyMap never evicts tiles on a dimension change, and one render with the new state poisons the region-image cache.
    @Inject(method = "center(Ljava/io/File;Ljourneymap/client/model/map/MapType;DDI)Z", at = @At("HEAD"))
    private void toroidal$dropTilesOnDimensionChange(CallbackInfoReturnable<Boolean> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        if (toroidal$lastLevelDimension != null && toroidal$lastLevelDimension != dimension) {
            this.clear();
            JourneyMapFold.gridDropped(toroidal$lastLevelDimension.identifier().toString(),
                    dimension.identifier().toString());
        }

        toroidal$lastLevelDimension = dimension;
    }

    @ModifyVariable(
            method = "center(Ljava/io/File;Ljourneymap/client/model/map/MapType;DDI)Z",
            at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double toroidal$foldCenterX(double blockX) {
        return JourneyMapFold.foldCenterCoord(Direction.Axis.X, blockX);
    }

    @ModifyVariable(
            method = "center(Ljava/io/File;Ljourneymap/client/model/map/MapType;DDI)Z",
            at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double toroidal$foldCenterZ(double blockZ) {
        return JourneyMapFold.foldCenterCoord(Direction.Axis.Z, blockZ);
    }

    @Inject(method = "getBlockPixelInGrid(Lnet/minecraft/core/BlockPos;)Ljava/awt/geom/Point2D$Double;",
            at = @At("HEAD"))
    private void toroidal$beginAnchorPass(CallbackInfoReturnable<Point2D.Double> cir) {
        toroidal$anchorPass = true;
    }

    @Inject(method = "getBlockPixelInGrid(Lnet/minecraft/core/BlockPos;)Ljava/awt/geom/Point2D$Double;",
            at = @At("RETURN"))
    private void toroidal$endAnchorPass(CallbackInfoReturnable<Point2D.Double> cir) {
        toroidal$anchorPass = false;
    }

    @ModifyVariable(method = "getBlockPixelInGrid(DD)Ljava/awt/geom/Point2D$Double;",
            at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private double toroidal$foldPixelX(double blockX) {
        return toroidal$anchorPass ? blockX
                : JourneyMapFold.nearestPixelCoord(Direction.Axis.X, this.centerBlockX, blockX);
    }

    @ModifyVariable(method = "getBlockPixelInGrid(DD)Ljava/awt/geom/Point2D$Double;",
            at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private double toroidal$foldPixelZ(double blockZ) {
        return toroidal$anchorPass ? blockZ
                : JourneyMapFold.nearestPixelCoord(Direction.Axis.Z, this.centerBlockZ, blockZ);
    }

    @Inject(method = "getCalculatedGridSize(I)I", at = @At("RETURN"), cancellable = true)
    private void toroidal$floorGridSizeToWorld(int zoom, CallbackInfoReturnable<Integer> cir) {
        int floor = JourneyMapFold.minGridSize();
        if (floor > cir.getReturnValue()) {
            cir.setReturnValue(floor);
        }
    }

    @ModifyVariable(method = "setZoom(D)Z", at = @At("HEAD"), argsOnly = true)
    private double toroidal$floorFullscreenZoom(double zoom) {
        return Context.UI.Fullscreen.equals(this.getUIState().ui) ? Math.max(zoom, JourneyMapFold.zoomFloor()) : zoom;
    }

    @Override
    public void toroidal$drawSeams(GuiGraphicsExtractor graphics, Matrix3x2fStack pose,
            MultiBufferSource.BufferSource buffers, double offsetX, double offsetZ) {
        if (!Context.UI.Fullscreen.equals(this.getUIState().ui) || JourneyMapFold.loopedAxes() == 0) {
            return;
        }

        Window window = Minecraft.getInstance().getWindow();
        int[] spanX = JourneyMapFold.viewSpan(this.centerBlockX, window.getWidth(), this.zoom);
        int[] spanZ = JourneyMapFold.viewSpan(this.centerBlockZ, window.getHeight(), this.zoom);
        int[] seamsX = JourneyMapFold.copies(Direction.Axis.X).seams(spanX[0], spanX[1]);
        int[] seamsZ = JourneyMapFold.copies(Direction.Axis.Z).seams(spanZ[0], spanZ[1]);

        VertexConsumer seamQuads = buffers.getBuffer(JMRenderTypes.RECTANGLE_RENDER_TYPE);
        for (int seam : seamsX) {
            int pixelX = (int) (this.getBlockPixelInGrid(new BlockPos(seam, 0, 0)).x + offsetX);
            toroidal$fillSeam(pose, seamQuads, pixelX, 0, 1, window.getHeight());
        }

        for (int seam : seamsZ) {
            int pixelZ = (int) (this.getBlockPixelInGrid(new BlockPos(0, 0, seam)).y + offsetZ);
            toroidal$fillSeam(pose, seamQuads, 0, pixelZ, window.getWidth(), 1);
        }
    }

    @Unique
    private static void toroidal$fillSeam(Matrix3x2fStack pose, VertexConsumer quads, int x, int y, int width,
            int height) {
        DrawUtil.drawRectangle(pose, quads, x, y, width, height, SEAM_ARGB & 0xFFFFFF, SEAM_ARGB >>> 24);
    }
}
