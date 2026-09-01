package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.compat.journeymap.JourneyMapFold;

import net.minecraft.core.Direction;

@Mixin(targets = "journeymap.client.model.region.RegionCoord", remap = false)
public class RegionCoordMixin {
    @ModifyVariable(method = "fromChunkPos", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private static int toroidal$foldChunkX(int chunkX) {
        return JourneyMapFold.foldRegionChunk(Direction.Axis.X, chunkX);
    }

    @ModifyVariable(method = "fromChunkPos", at = @At("HEAD"), ordinal = 1, argsOnly = true)
    private static int toroidal$foldChunkZ(int chunkZ) {
        return JourneyMapFold.foldRegionChunk(Direction.Axis.Z, chunkZ);
    }

    @ModifyVariable(method = "getXOffset", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int toroidal$foldOffsetChunkX(int chunkX) {
        return JourneyMapFold.foldRegionChunk(Direction.Axis.X, chunkX);
    }

    @ModifyVariable(method = "getZOffset", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int toroidal$foldOffsetChunkZ(int chunkZ) {
        return JourneyMapFold.foldRegionChunk(Direction.Axis.Z, chunkZ);
    }
}
