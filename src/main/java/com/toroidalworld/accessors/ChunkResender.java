package com.toroidalworld.accessors;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

// A teleport jumps the client mirror past the one-step increment ordinary movement keeps to, so a chunk the client
// still holds can now map to a different client-space copy. Vanilla's incremental send/forget assumes the copy never
// moves while held, so it neither re-sends the chunk at its new copy nor forgets it at the old one — leaving void where
// the chunk should be and a ghost a world away where it was. Fixing it is a forget-then-resend around the teleport, and
// the halves must straddle the mirror move: forget while the mirror is still old (so the forget names the copy the
// client actually holds), resend once it is new.
public interface ChunkResender {
    // Forget every chunk currently tracked for the player, translated around the CURRENT (still-old) mirror. Call before
    // the teleport moves it.
    void toroidal$dropTrackedChunks(ServerPlayer player);

    // Re-send every chunk in the player's view, translated around the (now-new) mirror. Call after the teleport.
    void toroidal$resendTrackedChunks(ServerPlayer player);

    // The surgical pair: forget and re-send only the named chunks, for a teleport that flips the copy of a few chunks
    // — or none — rather than the whole view. Same straddle contract as above: drop before the mirror moves, re-send
    // after. Chunks are named by their raw tracking-view coordinate, the same one vanilla's own view difference feeds
    // to its forget and send.
    void toroidal$dropChunks(ServerPlayer player, List<ChunkPos> chunks);

    void toroidal$resendChunks(ServerPlayer player, List<ChunkPos> chunks);
}
