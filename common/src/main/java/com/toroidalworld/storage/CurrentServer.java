package com.toroidalworld.storage;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.MinecraftServer;

// The running server, for the callers that are handed no level to ask — a saved map re-adding its decorations inside
// its own constructor is the one today. Published from the server thread's entry point, before any level exists, and
// cleared when the thread exits; volatile because the client thread may read it while an integrated server runs.
// On a client without an integrated server this stays null, which is the honest answer.
public final class CurrentServer {
    private static volatile @Nullable MinecraftServer current;

    public static @Nullable MinecraftServer get() {
        return current;
    }

    public static void set(MinecraftServer server) {
        current = server;
    }

    public static void clear() {
        current = null;
    }

    private CurrentServer() {
    }
}
