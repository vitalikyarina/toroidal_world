package com.toroidalworld.gen;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.world.level.chunk.status.ChunkStatus;

// A folded holder a foreign task declined to step and now waits on. Somebody has to generate it — a task may only run
// steps for holders in its own square — so the decline files this request and the chunk map answers it with a task of
// the holder's own on the next runGenerationTasks pass.
public record SeamDriveRequest(GenerationChunkHolder holder, ChunkStatus status) {
}
