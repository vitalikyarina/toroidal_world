package com.toroidalworld.compat.distanthorizons;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.MixinGatePlugin;

public class DhMixinPlugin extends MixinGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final boolean DH_PRESENT = DhMixinPlugin.class.getClassLoader()
            .getResource("com/seibel/distanthorizons/core/api/internal/ClientApi.class") != null;

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info("[dh-compat] gate distanthorizons_present={}", DH_PRESENT);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return DH_PRESENT;
    }
}
