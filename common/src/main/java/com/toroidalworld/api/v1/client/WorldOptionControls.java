package com.toroidalworld.api.v1.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.toroidalworld.core.StartupRegistry;
import com.toroidalworld.api.v1.option.WorldOption;
import com.toroidalworld.api.v1.option.WorldOptions;

/**
 * The client half of the world-option framework: which control an option is edited through, and the building of them
 * all for one screen. A declaration registers its factory here from the client side alone, beside the option itself.
 */
public final class WorldOptionControls {

    /** Builds the control for one option on one screen. */
    @FunctionalInterface
    public interface Factory {
        WorldOptionControl create(WorldOptionContext context);
    }

    private static final StartupRegistry<String, Factory> FACTORIES =
            new StartupRegistry<>("World option controls");

    /**
     * Registers the control {@code option} is edited through.
     *
     * @throws IllegalStateException if the registration boundary has already closed
     */
    public static void register(WorldOption<?> option, Factory factory) {
        FACTORIES.register(option.key(), factory);
    }

    /**
     * A control for every registered option that has one, in the order {@link WorldOptions#all} states — what a
     * shape's settings screen lays out. An option with no control registered is skipped.
     */
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
