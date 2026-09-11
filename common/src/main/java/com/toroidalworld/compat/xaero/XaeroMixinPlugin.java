package com.toroidalworld.compat.xaero;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.MixinGatePlugin;
import com.toroidalworld.compat.ModPresence;
import com.toroidalworld.compat.ModSymbol;

public class XaeroMixinPlugin extends MixinGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    static final ModSymbol WAYPOINT_SCALED_X =
            new ModSymbol("xaero/common/minimap/waypoints/Waypoint", "getX", "(D)I");

    static final ModSymbol WORLDMAP_CAMERA_X = new ModSymbol("xaero/map/gui/GuiMap", "cameraX", "D");

    private static final ModPresence XAERO_MINIMAP = ModPresence.of(LOGGER,
            "xaero/common/HudMod.class", "[xaero-compat] gate xaero_minimap_present", WAYPOINT_SCALED_X);

    private static final ModPresence XAERO_WORLDMAP = ModPresence.of(LOGGER,
            "xaero/map/WorldMap.class", "[xaero-compat] gate xaero_worldmap_present", WORLDMAP_CAMERA_X);

    private static final String WORLDMAP_MIXIN_PACKAGE = ".mixin.map.";

    @Override
    public void onLoad(String mixinPackage) {
        XAERO_MINIMAP.present();
        XAERO_WORLDMAP.present();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("SupportXaeroWorldmapMixin")) {
            return XAERO_MINIMAP.present() && XAERO_WORLDMAP.present();
        }

        return mixinClassName.contains(WORLDMAP_MIXIN_PACKAGE) ? XAERO_WORLDMAP.present() : XAERO_MINIMAP.present();
    }
}
