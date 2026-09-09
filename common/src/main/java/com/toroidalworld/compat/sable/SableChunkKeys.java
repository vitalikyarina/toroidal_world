package com.toroidalworld.compat.sable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public final class SableChunkKeys {
    public static ChunkPos physical(ServerLevel level, ChunkPos raw) {
        return physical(WorldLoopAttachments.transformerOf(level), raw);
    }

    public static ChunkPos physical(WorldFold fold, ChunkPos raw) {
        return fold.fold(raw);
    }

    private SableChunkKeys() {
    }
}
