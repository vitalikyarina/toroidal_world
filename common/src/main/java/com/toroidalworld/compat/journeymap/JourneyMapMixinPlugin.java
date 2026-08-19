package com.toroidalworld.compat.journeymap;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.mojang.logging.LogUtils;

public class JourneyMapMixinPlugin implements IMixinConfigPlugin {
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
