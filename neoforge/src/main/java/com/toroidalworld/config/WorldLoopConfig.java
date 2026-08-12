package com.toroidalworld.config;

import net.neoforged.neoforge.common.ModConfigSpec;

// The mod's client config (toroidal_world-client.toml), registered from the ToroidalWorld entrypoint. The spec lives
// with the feature it configures, not the entrypoint — the entrypoint only wires it, the same split as attachments and
// generators.
// Currently empty, kept as the wiring point for future client keys: FML silently skips registering an empty spec and
// the config-screen registration guards on SPEC.isEmpty(), so the toml and the mod-list Config button appear on their
// own with the first key.
public final class WorldLoopConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec SPEC = BUILDER.build();

    private WorldLoopConfig() {
    }
}
