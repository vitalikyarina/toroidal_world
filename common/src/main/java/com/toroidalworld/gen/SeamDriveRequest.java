package com.toroidalworld.gen;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public record SeamDriveRequest(GenerationChunkHolder holder, ChunkStatus status) {
}
