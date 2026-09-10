package com.toroidalworld.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold;
import com.toroidalworld.compat.ftbchunks.FtbChunksInjectionTargets;

import dev.ftb.mods.ftbchunks.client.minimap.components.ZoneInfoComponent;
import dev.ftb.mods.ftblibrary.math.XZ;

@Mixin(value = ZoneInfoComponent.class, remap = false)
public abstract class ZoneInfoComponentMixin {
    @WrapOperation(method = "shouldRender",
            at = @At(value = "INVOKE", target = FtbChunksInjectionTargets.XZ_REGION_FROM_CHUNK))
    private XZ toroidal$foldRegionKey(int chunkX, int chunkZ, Operation<XZ> original) {
        return FtbChunksFold.regionOfChunk(chunkX, chunkZ);
    }

    @WrapOperation(method = "shouldRender",
            at = @At(value = "INVOKE", target = FtbChunksInjectionTargets.XZ_OF))
    private XZ toroidal$foldChunkKey(int chunkX, int chunkZ, Operation<XZ> original) {
        return FtbChunksFold.chunkOf(chunkX, chunkZ);
    }
}
