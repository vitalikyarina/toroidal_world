package com.toroidalworld.engine.noise;

import java.util.Map;

import com.toroidalworld.core.StartupRegistry;
import com.toroidalworld.core.WorldFold;

import net.minecraft.world.level.levelgen.RandomState;

public final class GenerationHooks {
    @FunctionalInterface
    public interface RandomStateHook {
        void run(RandomState randomState, WorldFold fold, int seaLevel);
    }

    private static final StartupRegistry<String, RandomStateHook> RANDOM_STATE =
            new StartupRegistry<>("Generation hooks");

    public static void atRandomState(String key, RandomStateHook hook) {
        RANDOM_STATE.register(key, hook);
    }

    public static void runAtRandomState(RandomState randomState, WorldFold fold, int seaLevel) {
        RANDOM_STATE.entries().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> entry.getValue().run(randomState, fold, seaLevel));
    }

    private GenerationHooks() {
    }
}
