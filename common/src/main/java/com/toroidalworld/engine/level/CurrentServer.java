package com.toroidalworld.engine.level;

import org.jspecify.annotations.Nullable;

import net.minecraft.server.MinecraftServer;

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
