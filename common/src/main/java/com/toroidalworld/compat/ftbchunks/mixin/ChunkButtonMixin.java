package com.toroidalworld.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold;
import com.toroidalworld.compat.ftbchunks.FtbChunksInjectionTargets;

import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import dev.ftb.mods.ftbchunks.client.map.MapRegionData;
import dev.ftb.mods.ftblibrary.math.XZ;

@Mixin(targets = "dev.ftb.mods.ftbchunks.client.gui.ChunkScreenPanel$ChunkButton", remap = false)
public abstract class ChunkButtonMixin {
    @WrapOperation(method = "<init>",
            at = @At(value = "INVOKE", target = FtbChunksInjectionTargets.XZ_REGION_FROM_CHUNK))
    private XZ toroidal$foldRegionKey(int chunkX, int chunkZ, Operation<XZ> original) {
        return FtbChunksFold.regionOfChunk(chunkX, chunkZ);
    }

    @WrapOperation(method = "<init>",
            at = @At(value = "INVOKE", target = FtbChunksInjectionTargets.MAP_REGION_DATA_GET_CHUNK))
    private MapChunk toroidal$foldChunkKey(MapRegionData data, XZ chunk, Operation<MapChunk> original) {
        return original.call(data, FtbChunksFold.chunkOf(chunk));
    }
}
