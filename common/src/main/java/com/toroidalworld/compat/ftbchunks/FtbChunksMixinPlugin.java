package com.toroidalworld.compat.ftbchunks;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.MixinGatePlugin;
import com.toroidalworld.compat.ModPresence;

public class FtbChunksMixinPlugin extends MixinGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final boolean FTBCHUNKS_PRESENT = ModPresence.probe("dev/ftb/mods/ftbchunks/FTBChunks.class");

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info("[ftbc-compat] gate ftbchunks_present={}", FTBCHUNKS_PRESENT);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return FTBCHUNKS_PRESENT;
    }
}
