package com.toroidalworld.platform;

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
