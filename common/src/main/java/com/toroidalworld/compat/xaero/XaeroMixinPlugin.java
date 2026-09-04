package com.toroidalworld.compat.xaero;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.MixinGatePlugin;
import com.toroidalworld.compat.ModPresence;

public class XaeroMixinPlugin extends MixinGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final boolean XAERO_MINIMAP_PRESENT = ModPresence.probe("xaero/common/HudMod.class");
    private static final boolean XAERO_WORLDMAP_PRESENT = ModPresence.probe("xaero/map/WorldMap.class");

    private static final String WORLDMAP_MIXIN_PACKAGE = ".mixin.map.";

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info("[xaero-compat] gate xaero_minimap_present={} xaero_worldmap_present={}",
                XAERO_MINIMAP_PRESENT, XAERO_WORLDMAP_PRESENT);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("SupportXaeroWorldmapMixin")) {
            return XAERO_MINIMAP_PRESENT && XAERO_WORLDMAP_PRESENT;
        }

        return mixinClassName.contains(WORLDMAP_MIXIN_PACKAGE) ? XAERO_WORLDMAP_PRESENT : XAERO_MINIMAP_PRESENT;
    }
}
