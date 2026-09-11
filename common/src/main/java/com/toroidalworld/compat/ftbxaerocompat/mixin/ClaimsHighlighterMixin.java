package com.toroidalworld.compat.ftbxaerocompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.toroidalworld.compat.ftbchunks.FtbChunksFold;
import com.toroidalworld.compat.ftbchunks.FtbChunksInjectionTargets;
import com.toroidalworld.compat.ftbxaerocompat.FtbXaeroFold;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import dev.ftb.mods.ftblibrary.math.XZ;

@Mixin(targets = "dev.satherov.ftbxaerocompat.ClaimsHighlighter", remap = false)
public abstract class ClaimsHighlighterMixin {
    @WrapOperation(method = "getChunk",
            at = @At(value = "INVOKE", target = FtbChunksInjectionTargets.XZ_REGION_FROM_CHUNK))
    private static XZ toroidal$foldRegionKey(int chunkX, int chunkZ, Operation<XZ> original) {
        return FtbChunksFold.regionOfChunk(chunkX, chunkZ);
    }

    @WrapOperation(method = "getChunk",
            at = @At(value = "INVOKE", target = FtbChunksInjectionTargets.XZ_OF))
    private static XZ toroidal$foldChunkKey(int chunkX, int chunkZ, Operation<XZ> original) {
        return FtbChunksFold.chunkOf(chunkX, chunkZ);
    }

    @ModifyReturnValue(method = "regionHasHighlights", at = @At("RETURN"))
    private boolean toroidal$foldRegionHighlights(boolean original, ResourceKey<Level> key, int regionX, int regionZ) {
        return original || FtbXaeroFold.regionHasStoredClaims(regionX, regionZ);
    }
}
