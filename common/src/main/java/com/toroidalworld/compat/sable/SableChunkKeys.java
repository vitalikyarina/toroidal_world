package com.toroidalworld.compat.sable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public final class SableChunkKeys {
    public static ChunkPos physical(ServerLevel level, ChunkPos raw) {
        WorldFold fold = WorldLoopAttachments.wrappedTransformerOf(level);
        return fold == null ? raw : fold.fold(raw);
    }

    private SableChunkKeys() {
    }
}
