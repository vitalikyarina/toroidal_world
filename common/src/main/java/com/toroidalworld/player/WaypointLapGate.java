package com.toroidalworld.player;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

public final class WaypointLapGate {
    private @Nullable ChunkPos lastClientChunk;

    public int widen(int distance, ServerPlayer receiver, BlockPos serverBlock) {
        if (WorldLoopAttachments.wrappedTransformerOf(receiver.level()) == null) {
            return distance;
        }

        return widen(distance, receiver, ChunkPos.containing(serverBlock));
    }

    public int widen(int distance, ServerPlayer receiver, ChunkPos serverChunk) {
        WorldLoopTransformer transformer = WorldLoopAttachments.wrappedTransformerOf(receiver.level());
        if (transformer == null) {
            return distance;
        }

        ClientPosition mirror = WorldLoopAttachments.clientPositionOf(receiver);
        if (!mirror.describes(receiver.level().dimension())) {
            return distance;
        }

        ChunkPos projection = transformer.chunks.unwrap(mirror.chunk(), serverChunk);
        ChunkPos previous = this.lastClientChunk;
        this.lastClientChunk = projection;
        if (distance > 0 || previous == null || previous.equals(projection)) {
            return distance;
        }

        return 1;
    }
}
