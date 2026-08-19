package com.toroidalworld.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class WorldLoopConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec SPEC = BUILDER.build();

    private WorldLoopConfig() {
    }
}
