package com.toroidalworld.options;

import com.toroidalworld.platform.Platforms;
import com.toroidalworld.shape.torus.CompactBiomes;
import com.toroidalworld.shape.torus.GuaranteedLand;

public final class WorldOptionSetup {

    public static void registerAll() {
        registerAll(Platforms.get().isClient());
    }

    public static void registerAll(boolean client) {
        CompactBiomes.register(client);
        GuaranteedLand.register(client);
    }

    private WorldOptionSetup() {
    }
}
