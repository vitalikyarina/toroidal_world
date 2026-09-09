package com.toroidalworld.api.v1.option;

import java.util.Comparator;
import java.util.List;

import com.toroidalworld.core.StartupRegistry;

public final class WorldOptions {
    private static final StartupRegistry<String, WorldOption<?>> OPTIONS = new StartupRegistry<>("World options");

    public static <T> WorldOption<T> register(WorldOption<T> option) {
        OPTIONS.register(option.key(), option);
        return option;
    }

    public static List<WorldOption<?>> all() {
        return OPTIONS.entries().values().stream()
                .sorted(Comparator.comparingInt((WorldOption<?> option) -> option.position())
                        .thenComparing(option -> option.key()))
                .toList();
    }

    private WorldOptions() {
    }
}
