package com.toroidalworld.compat.sable;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

public final class SableChunkKeys {
    public static ChunkPos physical(ServerLevel level, ChunkPos raw) {
        return physical(WorldLoopAttachments.wrappedTransformerOf(level), raw);
    }

    public static ChunkPos physical(@Nullable WorldFold fold, ChunkPos raw) {
        return fold == null ? raw : fold.fold(raw);
    }

    private SableChunkKeys() {
    }
}
