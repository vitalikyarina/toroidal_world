package com.toroidalworld.storage;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.MinecraftServer;

// Published from the server thread's entry point and read by callers that are handed no level to ask.
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
