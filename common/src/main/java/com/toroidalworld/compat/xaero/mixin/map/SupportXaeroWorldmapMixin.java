package com.toroidalworld.compat.xaero.mixin.map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.toroidalworld.compat.xaero.XaeroInjectionTargets;
import com.toroidalworld.compat.xaero.XaeroWorldMapFold;

import net.minecraft.core.Direction;

import xaero.common.mods.SupportXaeroWorldmap;
import xaero.map.MapProcessor;
import xaero.map.WorldMapSession;
import xaero.map.region.MapRegion;
import xaero.map.region.MapTileChunk;

@Mixin(value = SupportXaeroWorldmap.class, remap = false)
public abstract class SupportXaeroWorldmapMixin {
    @Unique
    private int toroidal$fetchRegionX;
    @Unique
    private int toroidal$fetchRegionZ;
    @Unique
    private boolean toroidal$fetchIsLeaf;
    @Unique
    private int toroidal$fetchLeafLayer;
    @Unique
    private int toroidal$mirrorTileX;
    @Unique
    private int toroidal$mirrorTileZ;
    @Unique
    private MapRegion toroidal$foldedRegion;

    @Redirect(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/MapProcessor;getMinimapMapRegion(II)Lxaero/map/region/MapRegion;"))
    private MapRegion toroidal$fetchMinimapRegion(MapProcessor processor, int regX, int regZ) {
        this.toroidal$fetchRegionX = regX;
        this.toroidal$fetchRegionZ = regZ;
        this.toroidal$fetchIsLeaf = false;
        MapRegion original = processor.getMinimapMapRegion(regX, regZ);
        if (original != null || !XaeroWorldMapFold.active()) {
            return original;
        }

        // A candidate value only, so the null-guarded chunk fetch runs at all; the chunk redirect re-fetches precisely.
        return processor.getMinimapMapRegion(
                Math.floorDiv(XaeroWorldMapFold.foldTileChunk(Direction.Axis.X, regX * 8), 8),
                Math.floorDiv(XaeroWorldMapFold.foldTileChunk(Direction.Axis.Z, regZ * 8), 8));
    }

    @Redirect(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION))
    private MapRegion toroidal$fetchLeafRegion(MapProcessor processor, int caveLayer, int regX, int regZ, boolean create) {
        this.toroidal$fetchRegionX = regX;
        this.toroidal$fetchRegionZ = regZ;
        this.toroidal$fetchIsLeaf = true;
        this.toroidal$fetchLeafLayer = caveLayer;
        MapRegion original = processor.getLeafMapRegion(caveLayer, regX, regZ, create);
        if (original != null || !XaeroWorldMapFold.active()) {
            return original;
        }

        return processor.getLeafMapRegion(
                caveLayer,
                Math.floorDiv(XaeroWorldMapFold.foldTileChunk(Direction.Axis.X, regX * 8), 8),
                Math.floorDiv(XaeroWorldMapFold.foldTileChunk(Direction.Axis.Z, regZ * 8), 8),
                create);
    }

    @Redirect(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/MapRegion;getChunk(II)Lxaero/map/region/MapTileChunk;"))
    private MapTileChunk toroidal$fetchCanonicalChunk(MapRegion region, int localX, int localZ) {
        int mirrorTileX = this.toroidal$fetchRegionX * 8 + localX;
        int mirrorTileZ = this.toroidal$fetchRegionZ * 8 + localZ;
        this.toroidal$mirrorTileX = mirrorTileX;
        this.toroidal$mirrorTileZ = mirrorTileZ;
        this.toroidal$foldedRegion = null;
        if (!XaeroWorldMapFold.active()) {
            return region == null ? null : region.getChunk(localX, localZ);
        }

        int foldedTileX = XaeroWorldMapFold.foldTileChunk(Direction.Axis.X, mirrorTileX);
        int foldedTileZ = XaeroWorldMapFold.foldTileChunk(Direction.Axis.Z, mirrorTileZ);
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null) {
            return region == null ? null : region.getChunk(localX, localZ);
        }

        MapProcessor processor = session.getMapProcessor();
        int foldedRegionX = Math.floorDiv(foldedTileX, 8);
        int foldedRegionZ = Math.floorDiv(foldedTileZ, 8);
        MapRegion foldedRegion = this.toroidal$fetchIsLeaf
                ? processor.getLeafMapRegion(this.toroidal$fetchLeafLayer, foldedRegionX, foldedRegionZ, false)
                : processor.getMinimapMapRegion(foldedRegionX, foldedRegionZ);
        if (foldedRegion == null) {
            return null;
        }

        if (foldedRegion != region) {
            processor.beforeMinimapRegionRender(foldedRegion);
        }

        this.toroidal$foldedRegion = foldedRegion;
        return foldedRegion.getChunk(foldedTileX & 7, foldedTileZ & 7);
    }

    @Redirect(
            method = "renderChunks",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapTileChunk;getX()I"))
    private int toroidal$placeAtMirrorX(MapTileChunk chunk) {
        return XaeroWorldMapFold.active() ? this.toroidal$mirrorTileX : chunk.getX();
    }

    @Redirect(
            method = "renderChunks",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapTileChunk;getZ()I"))
    private int toroidal$placeAtMirrorZ(MapTileChunk chunk) {
        return XaeroWorldMapFold.active() ? this.toroidal$mirrorTileZ : chunk.getZ();
    }

    @Redirect(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/common/mods/SupportXaeroWorldmap;bumpLoadedRegion(Lxaero/map/MapProcessor;Lxaero/map/region/MapRegion;)V"))
    private void toroidal$bumpFoldedRegion(SupportXaeroWorldmap support, MapProcessor processor, MapRegion region) {
        MapRegion actual = this.toroidal$foldedRegion != null ? this.toroidal$foldedRegion : region;
        if (actual != null) {
            support.bumpLoadedRegion(processor, actual);
        }
    }
}
