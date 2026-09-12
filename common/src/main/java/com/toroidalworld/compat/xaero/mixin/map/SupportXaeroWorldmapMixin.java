package com.toroidalworld.compat.xaero.mixin.map;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
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

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/MapProcessor;getMinimapMapRegion(II)Lxaero/map/region/MapRegion;"))
    private @Nullable MapRegion toroidal$fetchMinimapRegion(MapProcessor processor, int regX, int regZ,
            Operation<@Nullable MapRegion> original) {
        this.toroidal$fetchRegionX = regX;
        this.toroidal$fetchRegionZ = regZ;
        this.toroidal$fetchIsLeaf = false;
        MapRegion existing = original.call(processor, regX, regZ);
        if (existing != null || !XaeroWorldMapFold.active()) {
            return existing;
        }

        // A candidate value only, so the null-guarded chunk fetch runs at all; the chunk redirect re-fetches precisely.
        return original.call(
                processor,
                XaeroWorldMapFold.foldRegion(Direction.Axis.X, regX),
                XaeroWorldMapFold.foldRegion(Direction.Axis.Z, regZ));
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION))
    private @Nullable MapRegion toroidal$fetchLeafRegion(MapProcessor processor, int caveLayer, int regX, int regZ,
            boolean create, Operation<@Nullable MapRegion> original) {
        this.toroidal$fetchRegionX = regX;
        this.toroidal$fetchRegionZ = regZ;
        this.toroidal$fetchIsLeaf = true;
        this.toroidal$fetchLeafLayer = caveLayer;
        MapRegion existing = original.call(processor, caveLayer, regX, regZ, create);
        if (existing != null || !XaeroWorldMapFold.active()) {
            return existing;
        }

        return original.call(
                processor,
                caveLayer,
                XaeroWorldMapFold.foldRegion(Direction.Axis.X, regX),
                XaeroWorldMapFold.foldRegion(Direction.Axis.Z, regZ),
                create);
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/MapRegion;getChunk(II)Lxaero/map/region/MapTileChunk;"))
    private @Nullable MapTileChunk toroidal$fetchCanonicalChunk(MapRegion region, int localX, int localZ,
            Operation<MapTileChunk> original) {
        int mirrorTileX = XaeroWorldMapFold.firstTileChunkOfRegion(this.toroidal$fetchRegionX) + localX;
        int mirrorTileZ = XaeroWorldMapFold.firstTileChunkOfRegion(this.toroidal$fetchRegionZ) + localZ;
        this.toroidal$mirrorTileX = mirrorTileX;
        this.toroidal$mirrorTileZ = mirrorTileZ;
        this.toroidal$foldedRegion = null;
        if (!XaeroWorldMapFold.active()) {
            return region == null ? null : original.call(region, localX, localZ);
        }

        int foldedTileX = XaeroWorldMapFold.foldTileChunk(Direction.Axis.X, mirrorTileX);
        int foldedTileZ = XaeroWorldMapFold.foldTileChunk(Direction.Axis.Z, mirrorTileZ);
        WorldMapSession session = WorldMapSession.getCurrentSession();
        if (session == null) {
            return region == null ? null : original.call(region, localX, localZ);
        }

        MapProcessor processor = session.getMapProcessor();
        int foldedRegionX = XaeroWorldMapFold.regionOfTileChunk(foldedTileX);
        int foldedRegionZ = XaeroWorldMapFold.regionOfTileChunk(foldedTileZ);
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
        return original.call(foldedRegion, XaeroWorldMapFold.tileChunkInRegion(foldedTileX),
                XaeroWorldMapFold.tileChunkInRegion(foldedTileZ));
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapTileChunk;getX()I"))
    private int toroidal$placeAtMirrorX(MapTileChunk chunk, Operation<Integer> original) {
        return XaeroWorldMapFold.active() ? this.toroidal$mirrorTileX : original.call(chunk);
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapTileChunk;getZ()I"))
    private int toroidal$placeAtMirrorZ(MapTileChunk chunk, Operation<Integer> original) {
        return XaeroWorldMapFold.active() ? this.toroidal$mirrorTileZ : original.call(chunk);
    }

    @WrapOperation(
            method = "renderChunks",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/common/mods/SupportXaeroWorldmap;bumpLoadedRegion(Lxaero/map/MapProcessor;Lxaero/map/region/MapRegion;)V"))
    private void toroidal$bumpFoldedRegion(SupportXaeroWorldmap support, MapProcessor processor, MapRegion region,
            Operation<Void> original) {
        MapRegion actual = this.toroidal$foldedRegion != null ? this.toroidal$foldedRegion : region;
        if (actual != null) {
            original.call(support, processor, actual);
        }
    }
}
