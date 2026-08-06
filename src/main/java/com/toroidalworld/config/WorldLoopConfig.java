package com.toroidalworld.config;

import net.neoforged.neoforge.common.ModConfigSpec;

// The mod's client config (toroidal_world-client.toml), registered from the ToroidalWorld entrypoint. The spec lives
// with the feature it configures, not the entrypoint — the entrypoint only wires it, the same split as attachments and
// generators.
public final class WorldLoopConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Off by default: the raw coordinate is a debugging aid — it is what the packet layer is lying about, and a mismatch
    // between it and the wrapped position is exactly what a translation bug would show. Now that the mod list has a
    // config screen, it is flipped on there when a seam issue is being chased rather than shown to everyone.
    public static final ModConfigSpec.BooleanValue SHOW_RAW_F3_COORDINATES = BUILDER
            .comment("Show the raw client coordinates in parentheses next to the wrapped ones in the F3 debug overlay.")
            .define("showRawCoordinatesInF3", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private WorldLoopConfig() {
    }
}
