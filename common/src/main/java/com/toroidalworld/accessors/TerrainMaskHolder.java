package com.toroidalworld.accessors;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.engine.gen.TerrainMask;

public interface TerrainMaskHolder {
    @Nullable TerrainMask toroidal$terrainMask();

    void toroidal$terrainMask(TerrainMask mask);
}
