package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.engine.noise.ClimateScaleCompression.Resolved;

public interface ClimateCompressionCache {
    @Nullable Resolved toroidal$climateCompression();

    void toroidal$climateCompression(Resolved resolved);
}
