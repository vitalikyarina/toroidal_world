package com.toroidalworld.platform;

import java.util.function.IntFunction;

import com.toroidalworld.options.WorldLoopBounds;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

// The loader seam, as one contract: everything the mod needs from its loader that has no vanilla call site to mixin
// into. Each loader ships one implementation and hands it to Platforms before anything else runs. Kept flat — one
// method per need; grouping them into sub-objects would be structure with a single tenant each.
public interface Platform {
    boolean isClient();

    // The version facts only the loader can state: the mod's own version as the loader read it off the jar's manifest
    // (so the build suffix a feature-branch jar is stamped with comes along), and which loader at which version runs
    // it. The game version is not here — SharedConstants answers that loader-free.
    String modVersion();

    String loaderName();

    String loaderVersion();

    // Sending is loader API (payload registration and dispatch); what to send and when stays in common — see
    // WrappingBoundsSync.
    void sendWrappingBounds(ServerPlayer player, WorldLoopBounds bounds);

    // The buffer a rewriter re-encodes a packet through (capacity in bytes) has to write the wire format the receiving
    // connection itself would use — a modded and a vanilla client do not share one, and which buffer that is only the
    // loader knows.
    IntFunction<RegistryFriendlyByteBuf> packetBuffers(ServerPlayer player);
}
