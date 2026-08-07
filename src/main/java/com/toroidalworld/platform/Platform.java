package com.toroidalworld.platform;

import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.server.level.ServerPlayer;

// The loader seam, as one contract: everything the mod needs from its loader that has no vanilla call site to mixin
// into. Each loader ships one implementation and hands it to Platforms before anything else runs. Kept flat — one
// method per need — because the needs are three; grouping them into network/config sub-objects would be structure
// with a single tenant each.
public interface Platform {
    boolean isClient();

    // Sending is loader API (payload registration and dispatch); what to send and when stays in common — see
    // WrappingBoundsSync.
    void sendWrappingBounds(ServerPlayer player, WorldLoopBounds bounds);

    boolean showRawF3Coordinates();
}
