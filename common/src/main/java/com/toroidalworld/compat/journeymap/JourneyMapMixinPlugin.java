package com.toroidalworld.compat.journeymap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.MixinGatePlugin;
import com.toroidalworld.compat.ModPresence;
import com.toroidalworld.compat.ModSymbol;

public class JourneyMapMixinPlugin extends MixinGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    static final ModSymbol MAP_RENDERER_CENTRE =
            new ModSymbol("journeymap/client/render/map/MapRenderer", "centerBlockX", "D");

    private static final ModPresence JOURNEYMAP = ModPresence.of(LOGGER,
            "journeymap/client/JourneymapClient.class", "[jm-compat] gate jm_present", MAP_RENDERER_CENTRE);

    @Override
    public void onLoad(String mixinPackage) {
        JOURNEYMAP.present();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return JOURNEYMAP.present();
    }
}
