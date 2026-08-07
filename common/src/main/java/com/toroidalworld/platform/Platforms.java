package com.toroidalworld.platform;

// Filled once by the loader entrypoint while the mod initializes — before any level, connection or screen exists —
// then only read. Not volatile for the same reason the transformer caches are not: the write happens-before every
// reader through mod loading itself.
public final class Platforms {
    private static Platform platform;

    public static void set(Platform chosen) {
        platform = chosen;
    }

    public static Platform get() {
        if (platform == null) {
            throw new IllegalStateException("Platform is not set — the loader entrypoint must call Platforms.set first.");
        }

        return platform;
    }

    private Platforms() {
    }
}
