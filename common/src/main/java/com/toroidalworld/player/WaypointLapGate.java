package com.toroidalworld.player;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.storage.WorldLoopAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

// Vanilla resends a waypoint only when the source moved in server space. The receiver's client lives in an unbounded
// mirror space, so when the receiver laps the seam, the copy of a *static* source nearest them changes by a whole
// world — and nothing resends, leaving the arrow aimed at the stale copy. This gate widens the update decision for the
// block- and chunk-grained connections alike: when the waypoint's projection into the receiver's space flips, the send
// is forced and the packet translation lays the waypoint back down beside them.
//
// The projection only ever moves by whole world widths while the source stands still, so chunk grain is enough for
// both connection kinds. One gate belongs to one connection, like the projection memory it keeps.
public final class WaypointLapGate {
    private @Nullable ChunkPos lastClientChunk;

    // The block-grained connection hands in the position it holds; the chunk conversion waits until the level is known
    // to wrap, so an unwrapped world pays no allocation for it.
    public int widen(int distance, ServerPlayer receiver, BlockPos serverBlock) {
        if (WorldLoopAttachments.wrappedTransformerOf(receiver.level()) == null) {
            return distance;
        }

        return widen(distance, receiver, ChunkPos.containing(serverBlock));
    }

    // The vanilla distance passes through untouched; 1 stands in for it only to force the resend vanilla would skip.
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
