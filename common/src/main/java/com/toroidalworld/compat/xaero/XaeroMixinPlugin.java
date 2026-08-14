package com.toroidalworld.compat.xaero;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.mojang.logging.LogUtils;

// Gates the Xaero compat mixins on the respective mod actually being installed — the two Xaero mods ship
// separately, so each has its own probe: HudMod is the minimap's entry class, WorldMap the world map's, and the
// mixins under the .map subpackage belong to the latter. Detection is a classpath resource probe rather than a
// loader API or Class.forName: mixin config plugins run before mod initialization on both loaders, and looking a
// .class resource up loads nothing.
public class XaeroMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final boolean XAERO_MINIMAP_PRESENT = XaeroMixinPlugin.class.getClassLoader()
            .getResource("xaero/common/HudMod.class") != null;
    private static final boolean XAERO_WORLDMAP_PRESENT = XaeroMixinPlugin.class.getClassLoader()
            .getResource("xaero/map/WorldMap.class") != null;

    private static final String WORLDMAP_MIXIN_PACKAGE = ".mixin.map.";

    @Override
    public void onLoad(String mixinPackage) {
        LOGGER.info("[xaero-compat] gate xaero_minimap_present={} xaero_worldmap_present={}",
                XAERO_MINIMAP_PRESENT, XAERO_WORLDMAP_PRESENT);
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // The world-map bridge class lives in the minimap jar but leans on world-map types — it needs both mods.
        if (mixinClassName.endsWith("SupportXaeroWorldmapMixin")) {
            return XAERO_MINIMAP_PRESENT && XAERO_WORLDMAP_PRESENT;
        }

        return mixinClassName.contains(WORLDMAP_MIXIN_PACKAGE) ? XAERO_WORLDMAP_PRESENT : XAERO_MINIMAP_PRESENT;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
