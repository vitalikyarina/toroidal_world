package com.toroidalworld.compat.journeymap.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.toroidalworld.compat.journeymap.JourneyMapFold;

import net.minecraft.core.Direction;

// The root of the endless-strip defect: every region file JourneyMap writes or reads is keyed by chunkX >> 5 inside
// this class, and the chunk coordinates arriving here are the client's unbounded mirror. Folding them at this one
// factory makes every RegionCoord canonical — a lap paints back into the files it painted the first time. The offset
// getters take the same fold because the chunks handed to them still carry mirror coordinates, and unfolded they
// throw on any chunk past the bounds.
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
