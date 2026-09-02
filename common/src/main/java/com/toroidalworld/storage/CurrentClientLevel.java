package com.toroidalworld.storage;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.level.Level;

// Published from client-only code alone — a publish from common class-loads Minecraft on a dedicated server.
public final class CurrentClientLevel {
    private static volatile @Nullable Supplier<@Nullable Level> current;

    public static @Nullable Level get() {
        Supplier<@Nullable Level> supplier = current;
        return supplier == null ? null : supplier.get();
    }

    public static void publish(Supplier<@Nullable Level> supplier) {
        current = supplier;
    }

    private CurrentClientLevel() {
    }
}
