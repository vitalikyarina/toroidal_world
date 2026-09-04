package com.toroidalworld.compat.journeymap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.MixinGatePlugin;

public class JourneyMapMixinPlugin extends MixinGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final boolean JOURNEYMAP_PRESENT = JourneyMapMixinPlugin.class.getClassLoader()
            .getResource("journeymap/client/JourneymapClient.class") != null;

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info("[jm-compat] gate jm_present={}", JOURNEYMAP_PRESENT);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return JOURNEYMAP_PRESENT;
    }
}
