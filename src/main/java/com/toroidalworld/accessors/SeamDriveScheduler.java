package com.toroidalworld.accessors;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.world.level.chunk.status.ChunkStatus;

// Duck on ChunkMap: file a request to give this holder a generation task of its own. Called from worker threads at the
// moment a foreign task declines to step a cross-seam holder; drained on the server thread, the only one allowed to
// touch the pending-task queue.
public interface SeamDriveScheduler {
    void toroidal$requestDrive(GenerationChunkHolder holder, ChunkStatus status);
}
