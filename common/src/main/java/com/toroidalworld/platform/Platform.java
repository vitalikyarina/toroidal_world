package com.toroidalworld.platform;

import java.util.function.IntFunction;

import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

// The loader seam, as one contract: everything the mod needs from its loader that has no vanilla call site to mixin
// into. Each loader ships one implementation and hands it to Platforms before anything else runs. Kept flat — one
// method per need — because the needs are two; grouping them into network sub-objects would be structure with a
// single tenant each.
public interface Platform {
    boolean isClient();

    // Sending is loader API (payload registration and dispatch); what to send and when stays in common — see
    // WrappingBoundsSync.
    void sendWrappingBounds(ServerPlayer player, WorldLoopBounds bounds);

    // The buffer a rewriter re-encodes a packet through (capacity in bytes) has to write the wire format the receiving
    // connection itself would use — a modded and a vanilla client do not share one, and which buffer that is only the
    // loader knows.
    IntFunction<RegistryFriendlyByteBuf> packetBuffers(ServerPlayer player);
}
