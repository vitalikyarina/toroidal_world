package com.toroidalworld.compat.create;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.sable.SableMod;

public class CreateMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String BLUEPRINT_REACH_MIXIN =
            "com.toroidalworld.compat.create.mixin.BlueprintReachMixin";

    @Override
    public void onLoad(String mixinPackage) {
        // Evaluated here rather than left to the first shouldApplyMixin call: the gate logs the answer as it probes,
        // and with the config carrying no mixin yet nothing would ever query it — so the one line saying whether
        // Create was seen would be missing exactly on the run that has to prove the module is inert without it.
        CreateMod.present();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!CreateMod.present()) {
            return false;
        }

        if (BLUEPRINT_REACH_MIXIN.equals(mixinClassName)) {
            // Mixin refuses any injection into a method another mixin merged, so applying this beside Sable's
            // canPlayerUse overwrite drops the whole class and takes the two unrelated blueprint folds with it.
            boolean sablePresent = SableMod.present();
            LOGGER.info("[create-compat] gate blueprint_reach sable_present={} applied={}", sablePresent,
                    !sablePresent);
            return !sablePresent;
        }

        return true;
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
