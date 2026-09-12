package com.toroidalworld.compat.xaero.mixin.map;

import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.xaero.XaeroExportAssembly;
import com.toroidalworld.compat.xaero.XaeroInjectionTargets;

import net.minecraft.core.Registry;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;

import xaero.map.MapProcessor;
import xaero.map.file.OldFormatSupport;
import xaero.map.file.RegionDetection;
import xaero.map.file.export.PNGExportResult;
import xaero.map.file.export.PNGExporter;
import xaero.map.gui.MapTileSelection;
import xaero.map.highlight.DimensionHighlighterHandler;
import xaero.map.region.ExportMapRegion;
import xaero.map.region.ExportMapTileChunk;
import xaero.map.region.MapLayer;
import xaero.map.region.MapRegion;
import xaero.map.world.MapDimension;

@Mixin(value = PNGExporter.class, remap = false)
public abstract class PNGExporterMixin {
    @Unique
    private final XaeroExportAssembly toroidal$assembly = new XaeroExportAssembly();

    @Inject(method = "export", at = @At("HEAD"))
    private void toroidal$beginExport(MapProcessor processor, Registry<Biome> biomes,
            Registry<DimensionType> dimensionTypes, MapTileSelection selection, OldFormatSupport oldFormatSupport,
            CallbackInfoReturnable<PNGExportResult> cir) {
        this.toroidal$assembly.begin(processor, biomes, oldFormatSupport);
    }

    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = XaeroInjectionTargets.MAP_PROCESSOR_GET_LEAF_MAP_REGION))
    private MapRegion toroidal$fetchSourceRegion(MapProcessor processor, int caveLayer, int regionX, int regionZ,
            boolean create, Operation<MapRegion> original) {
        this.toroidal$assembly.enterRegion(regionX, regionZ);
        return original.call(processor, caveLayer, this.toroidal$assembly.regionX(), this.toroidal$assembly.regionZ(),
                create);
    }

    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/MapLayer;regionDetectionExists(II)Z"))
    private boolean toroidal$detectSourceRegion(MapLayer layer, int regionX, int regionZ,
            Operation<Boolean> original) {
        return original.call(layer, this.toroidal$assembly.regionX(), this.toroidal$assembly.regionZ());
    }

    @WrapOperation(
            method = "export",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/MapLayer;getRegionDetection(II)Lxaero/map/file/RegionDetection;"))
    private RegionDetection toroidal$fetchSourceDetection(MapLayer layer, int regionX, int regionZ,
            Operation<RegionDetection> original) {
        return original.call(layer, this.toroidal$assembly.regionX(), this.toroidal$assembly.regionZ());
    }

    @WrapOperation(
            method = "export",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/highlight/DimensionHighlighterHandler;shouldApplyRegionHighlights(IIZ)Z"))
    private boolean toroidal$sourceRegionHighlights(DimensionHighlighterHandler handler, int regionX, int regionZ,
            boolean discovered, Operation<Boolean> original) {
        return original.call(handler, this.toroidal$assembly.regionX(), this.toroidal$assembly.regionZ(), discovered);
    }

    @WrapOperation(method = "export", at = @At(value = "NEW", target = "xaero/map/region/ExportMapRegion"))
    private ExportMapRegion toroidal$buildSourceRegion(MapDimension dimension, int regionX, int regionZ,
            int caveLayer, Registry<Biome> biomes, Operation<ExportMapRegion> original) {
        ExportMapRegion region = original.call(dimension, this.toroidal$assembly.regionX(),
                this.toroidal$assembly.regionZ(), caveLayer, biomes);
        this.toroidal$assembly.adopt(region);
        return region;
    }

    @WrapOperation(
            method = "export",
            at = @At(
                    value = "INVOKE",
                    target = "Lxaero/map/region/ExportMapRegion;getChunk(II)Lxaero/map/region/ExportMapTileChunk;"))
    private @Nullable ExportMapTileChunk toroidal$resolveSlot(ExportMapRegion region, int slotX, int slotZ,
            Operation<ExportMapTileChunk> original) {
        if (!this.toroidal$assembly.assembling()) {
            return original.call(region, slotX, slotZ);
        }

        return this.toroidal$assembly.chunk(slotX, slotZ);
    }

    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/ExportMapTileChunk;getX()I"))
    private int toroidal$slotX(ExportMapTileChunk chunk, Operation<Integer> original) {
        return this.toroidal$assembly.assembling() ? this.toroidal$assembly.tileChunkX() : original.call(chunk);
    }

    @WrapOperation(
            method = "export",
            at = @At(value = "INVOKE", target = "Lxaero/map/region/ExportMapTileChunk;getZ()I"))
    private int toroidal$slotZ(ExportMapTileChunk chunk, Operation<Integer> original) {
        return this.toroidal$assembly.assembling() ? this.toroidal$assembly.tileChunkZ() : original.call(chunk);
    }

    @Inject(method = "export", at = @At("RETURN"))
    private void toroidal$endExport(CallbackInfoReturnable<PNGExportResult> cir) {
        this.toroidal$assembly.end();
    }
}
