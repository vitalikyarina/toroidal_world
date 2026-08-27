package com.toroidalworld.compat.journeymap.mixin;

import java.awt.geom.Point2D;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.journeymap.JourneyMapFold;

import journeymap.api.v2.client.display.Context;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

@Mixin(targets = "journeymap.client.render.map.MapRenderer", remap = false)
public abstract class MapRendererMixin {
    @Shadow(remap = false)
    protected double centerBlockX;

    @Shadow(remap = false)
    protected double centerBlockZ;

    @Shadow(remap = false)
    public abstract void clear();

    @Shadow(remap = false)
    protected int zoom;

    @Shadow(remap = false)
    @Final
    protected Context.UI contextUi;

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
            JourneyMapFold.gridDropped(toroidal$lastLevelDimension.location().toString(),
                    dimension.location().toString());
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

    @WrapOperation(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDFZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Ljourneymap/client/render/map/RegionTile;render(Lnet/minecraft/client/gui/GuiGraphics;"
                            + "Lnet/minecraft/client/renderer/MultiBufferSource;DDFLjourneymap/client/model/map/MapType;I)V"))
    private void toroidal$renderWrappedCopies(@Coerce Object tile, GuiGraphics graphics, MultiBufferSource buffers,
            double pixelOffsetX, double pixelOffsetZ, float alpha, @Coerce Object mapType, int shaderIndex,
            Operation<Void> original) {
        original.call(tile, graphics, buffers, pixelOffsetX, pixelOffsetZ, alpha, mapType, shaderIndex);
        if (this.contextUi == Context.UI.Webmap) {
            return;
        }

        int loopedAxes = JourneyMapFold.loopedAxes();
        double periodX = JourneyMapFold.worldPixelPeriod(Direction.Axis.X, this.zoom);
        double periodZ = JourneyMapFold.worldPixelPeriod(Direction.Axis.Z, this.zoom);
        int rangeX = JourneyMapFold.copyRange(loopedAxes, periodX, graphics.guiWidth());
        int rangeZ = JourneyMapFold.copyRange(loopedAxes, periodZ, graphics.guiHeight());
        if (this.contextUi == Context.UI.Fullscreen) {
            rangeX = Math.min(rangeX, JourneyMapFold.fullscreenCopyRange(Direction.Axis.X));
            rangeZ = Math.min(rangeZ, JourneyMapFold.fullscreenCopyRange(Direction.Axis.Z));
        }

        JourneyMapFold.logTileCopies(this.contextUi.name(), this.zoom, loopedAxes, periodX, periodZ,
                graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(), graphics.guiHeight());
        if (rangeX == 0 && rangeZ == 0) {
            return;
        }

        for (int lapX = -rangeX; lapX <= rangeX; lapX++) {
            for (int lapZ = -rangeZ; lapZ <= rangeZ; lapZ++) {
                if (lapX == 0 && lapZ == 0) {
                    continue;
                }

                original.call(tile, graphics, buffers,
                        pixelOffsetX + lapX * periodX, pixelOffsetZ + lapZ * periodZ, alpha, mapType, shaderIndex);
            }
        }
    }
}
