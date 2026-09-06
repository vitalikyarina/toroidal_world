package com.toroidalworld.compat.aeronautics;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.MixinGatePlugin;

public class AeronauticsMixinPlugin extends MixinGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public void onLoad(String mixinPackage) {
        for (BundleMod mod : BundleMod.values()) {
            mod.present();
        }
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        BundleMod owner = BundleMod.owning(targetClassName);

        if (owner == null) {
            LOGGER.warn("[aeronautics-compat] gate unknown_target target={} mixin={}", targetClassName, mixinClassName);

            return false;
        }

        return owner.present();
    }
}
