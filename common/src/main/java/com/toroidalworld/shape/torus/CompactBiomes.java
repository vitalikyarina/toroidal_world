package com.toroidalworld.shape.torus;

import com.toroidalworld.client.options.WorldOptionControls;
import com.toroidalworld.client.shape.torus.CompactBiomesControl;
import com.toroidalworld.core.WorldOption;
import com.toroidalworld.core.WorldOptions;

public final class CompactBiomes {
    public static final String KEY = "climate_compression";

    private static final int POSITION = 0;

    public static final WorldOption<ClimateScale> OPTION = new WorldOption<>(
            KEY, POSITION, ClimateScale.CODEC, ClimateScale.AUTO);

    public static void register(boolean client) {
        WorldOptions.register(OPTION);

        if (client) {
            WorldOptionControls.register(OPTION, CompactBiomesControl::new);
        }
    }

    private CompactBiomes() {
    }
}
