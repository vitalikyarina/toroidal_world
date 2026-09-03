package com.toroidalworld.compat.distanthorizons;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.mojang.logging.LogUtils;

public class DhMixinPlugin implements IMixinConfigPlugin {
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
