package com.toroidalworld.compat.journeymap.mixin;

import java.awt.geom.Point2D;
import java.io.File;
import java.util.Collection;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.Window;
import com.toroidalworld.compat.MapCopyBudget;
import com.toroidalworld.compat.journeymap.JourneyMapFold;

import journeymap.api.v2.client.display.Context;
import journeymap.api.v2.client.util.UIState;
import journeymap.client.model.map.MapType;
import journeymap.client.model.region.RegionCoord;
import journeymap.client.model.region.RegionImageCache;
import journeymap.client.model.region.RegionImageSet;
import journeymap.client.render.map.RegionTile;
import journeymap.client.render.map.TileGrid;
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
    protected int zoom;

    @Shadow(remap = false)
    private volatile File worldDir;

    @Shadow(remap = false)
    @Final
    TileGrid<RegionCoord, RegionTile> regions;

    @Shadow(remap = false)
    public abstract void clear();

    @Shadow(remap = false)
    @Final
    protected Context.UI contextUi;

    @Shadow(remap = false)
    public abstract UIState getUIState();

    // Render-thread only, like every caller of these methods.
    @Unique
    private static boolean toroidal$anchorPass;

    @Unique
    private ResourceKey<Level> toroidal$lastLevelDimension;

    @Unique
    private File toroidal$lastWorldDir;

    @Inject(method = "center(Ljava/io/File;Ljourneymap/client/model/map/MapType;DDI)Z", at = @At("HEAD"))
    private void toroidal$dropTilesOnWorldChange(File worldDir, MapType mapType, double blockX, double blockZ,
            int zoom, CallbackInfoReturnable<Boolean> cir) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        ResourceKey<Level> dimension = level.dimension();
        String reason = JourneyMapFold.staleGridReason(toroidal$lastLevelDimension, dimension,
                toroidal$lastWorldDir, worldDir);
        if (reason != null) {
            boolean byWorld = JourneyMapFold.WORLD_CHANGED.equals(reason);
            JourneyMapFold.gridDropped(reason,
                    byWorld ? toroidal$lastWorldDir.getName() : toroidal$lastLevelDimension.location().toString(),
                    byWorld ? worldDir.getName() : dimension.location().toString(),
                    this.regions.size());
            this.clear();
        }

        toroidal$lastLevelDimension = dimension;
        if (worldDir != null) {
            toroidal$lastWorldDir = worldDir;
        }
    }

    @WrapOperation(
            method = "loadInMemoryRegions",
            at = @At(value = "INVOKE",
                    target = "Ljourneymap/client/model/region/RegionImageCache;getRegionImageSets()Ljava/util/Collection;"))
    private Collection<RegionImageSet> toroidal$onlyThisWorldsRegions(RegionImageCache cache,
            Operation<Collection<RegionImageSet>> original) {
        Collection<RegionImageSet> sets = original.call(cache);
        File dir = this.worldDir;
        if (dir == null) {
            return sets;
        }

        return sets.stream().filter(set -> dir.equals(set.getRegionCoord().worldDir)).toList();
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

    @ModifyReturnValue(method = "getCalculatedGridSize(I)I", at = @At("RETURN"))
    private int toroidal$floorGridSizeToWorld(int original) {
        return Math.max(original, JourneyMapFold.minGridSize());
    }

    @ModifyVariable(method = "setZoom(D)Z", at = @At("HEAD"), argsOnly = true)
    private double toroidal$floorFullscreenZoom(double zoom) {
        return Context.UI.Fullscreen.equals(this.getUIState().ui) ? Math.max(zoom, JourneyMapFold.zoomFloor()) : zoom;
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
        if (loopedAxes == 0) {
            return;
        }

        double periodX = JourneyMapFold.worldPixelPeriod(Direction.Axis.X, this.zoom);
        double periodZ = JourneyMapFold.worldPixelPeriod(Direction.Axis.Z, this.zoom);
        Window window = Minecraft.getInstance().getWindow();
        int tiles = JourneyMapFold.tilesWithContent(this.zoom, window.getWidth(), window.getHeight());
        int rangeX = MapCopyBudget.copyRange(loopedAxes, tiles, periodX, window.getWidth());
        int rangeZ = MapCopyBudget.copyRange(loopedAxes, tiles, periodZ, window.getHeight());
        if (this.contextUi == Context.UI.Fullscreen) {
            JourneyMapFold.recordFullscreenCopyRange(rangeX, rangeZ);
        }

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
