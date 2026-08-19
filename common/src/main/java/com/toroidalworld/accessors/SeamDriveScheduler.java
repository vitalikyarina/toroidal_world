package com.toroidalworld.accessors;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.world.level.chunk.status.ChunkStatus;

// Filed from worker threads when a foreign task declines a cross-seam holder, drained on the server thread.
public interface SeamDriveScheduler {
    void toroidal$requestDrive(GenerationChunkHolder holder, ChunkStatus status);
}
