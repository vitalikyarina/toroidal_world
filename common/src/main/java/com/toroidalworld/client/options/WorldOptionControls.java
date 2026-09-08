package com.toroidalworld.client.options;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.toroidalworld.core.StartupRegistry;
import com.toroidalworld.core.WorldOption;
import com.toroidalworld.core.WorldOptions;

public final class WorldOptionControls {
    @FunctionalInterface
    public interface Factory {
        WorldOptionControl create(WorldOptionContext context);
    }

    private static final StartupRegistry<String, Factory> FACTORIES =
            new StartupRegistry<>("World option controls");

    public static void register(WorldOption<?> option, Factory factory) {
        FACTORIES.register(option.key(), factory);
    }

    public static List<WorldOptionControl> createAll(WorldOptionContext context) {
        Map<String, Factory> factories = FACTORIES.entries();
        List<WorldOptionControl> controls = new ArrayList<>();
        for (WorldOption<?> option : WorldOptions.all()) {
            Factory factory = factories.get(option.key());
            if (factory != null) {
                controls.add(factory.create(context));
            }
        }

        return List.copyOf(controls);
    }

    private WorldOptionControls() {
    }
}
