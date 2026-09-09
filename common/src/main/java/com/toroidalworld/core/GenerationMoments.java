package com.toroidalworld.core;

import java.util.Map;

import com.toroidalworld.api.v1.ToroidalShape;
import com.toroidalworld.api.v1.gen.GenerationHooks;
import com.toroidalworld.api.v1.option.GenerationOptions;

import net.minecraft.world.level.levelgen.RandomState;

public final class GenerationMoments {
    private static final StartupRegistry<String, GenerationHooks.RandomStateHook> RANDOM_STATE =
            new StartupRegistry<>("Generation hooks");

    public static void atRandomState(String key, GenerationHooks.RandomStateHook hook) {
        RANDOM_STATE.register(key, hook);
    }

    public static void runAtRandomState(RandomState randomState, ToroidalShape shape, GenerationOptions options,
            int seaLevel) {
        RANDOM_STATE.entries().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> entry.getValue().run(randomState, shape, options, seaLevel));
    }

    private GenerationMoments() {
    }
}
