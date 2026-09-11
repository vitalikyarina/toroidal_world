package com.toroidalworld.compat.ftbxaerocompat;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.MixinGatePlugin;
import com.toroidalworld.compat.ModPresence;

public class FtbXaeroMixinPlugin extends MixinGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final boolean FTBXAERO_PRESENT =
            ModPresence.probe("dev/satherov/ftbxaerocompat/FTBXaeroCompat.class");

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info("[ftbxaero-compat] gate ftbxaerocompat_present={}", FTBXAERO_PRESENT);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return FTBXAERO_PRESENT;
    }
}
