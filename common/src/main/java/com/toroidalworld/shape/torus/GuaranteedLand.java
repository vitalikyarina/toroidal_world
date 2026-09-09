package com.toroidalworld.shape.torus;

import com.toroidalworld.client.options.WorldOptionControls;
import com.toroidalworld.client.shape.torus.GuaranteedLandControl;
import com.toroidalworld.core.WorldOption;
import com.toroidalworld.core.WorldOptions;

import com.mojang.serialization.Codec;

public final class GuaranteedLand {
    public static final String KEY = "guaranteed_land";

    private static final int POSITION = 1;

    private static final boolean OFF = false;

    public static final WorldOption<Boolean> OPTION = new WorldOption<>(
            KEY, POSITION, Codec.BOOL, OFF);

    public static void register(boolean client) {
        WorldOptions.register(OPTION);

        if (client) {
            WorldOptionControls.register(OPTION, GuaranteedLandControl::new);
        }
    }

    private GuaranteedLand() {
    }
}
