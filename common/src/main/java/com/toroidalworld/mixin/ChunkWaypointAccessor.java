package com.toroidalworld.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.level.ChunkPos;

@Mixin(targets = "net.minecraft.world.waypoints.TrackedWaypoint$ChunkWaypoint")
public interface ChunkWaypointAccessor {
    @Accessor("chunkPos")
    ChunkPos toroidal$getChunkPos();

    @Accessor("chunkPos")
    void toroidal$setChunkPos(ChunkPos chunkPos);
}
