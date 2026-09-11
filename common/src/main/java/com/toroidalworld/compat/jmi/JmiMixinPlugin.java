package com.toroidalworld.compat.jmi;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.MixinGatePlugin;
import com.toroidalworld.compat.ModPresence;

public class JmiMixinPlugin extends MixinGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final boolean JMI_PRESENT = ModPresence.probe("me/frankv/jmi/JMI.class");

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info("[jmi-compat] gate jmi_present={}", JMI_PRESENT);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return JMI_PRESENT;
    }
}
