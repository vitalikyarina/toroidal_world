package com.toroidalworld.compat.ftbchunks.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold;

import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import dev.ftb.mods.ftblibrary.math.XZ;

@Mixin(value = MapChunk.class, remap = false)
public abstract class MapChunkMixin {
    @ModifyExpressionValue(method = "offsetBlocking",
            at = @At(value = "INVOKE",
                    target = "Ldev/ftb/mods/ftblibrary/math/XZ;offset(II)Ldev/ftb/mods/ftblibrary/math/XZ;"))
    private XZ toroidal$foldNeighbour(XZ neighbour) {
        return FtbChunksFold.chunkOf(neighbour);
    }
}
