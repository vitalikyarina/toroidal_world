package com.toroidalworld;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

// The mod's identity — id and logger — shared by every module. The entrypoints are loader-side classes
// (ToroidalWorldNeoForge) that call WorldLoop.init and know nothing else about the feature's internals.
public final class ToroidalWorld {
    public static final String MODID = "toroidal_world";
    public static final Logger LOGGER = LogUtils.getLogger();

    private ToroidalWorld() {
    }
}
