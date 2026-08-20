package com.toroidalworld.compat.create;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class CreateMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        // Evaluated here rather than left to the first shouldApplyMixin call: the gate logs the answer as it probes,
        // and with the config carrying no mixin yet nothing would ever query it — so the one line saying whether
        // Create was seen would be missing exactly on the run that has to prove the module is inert without it.
        CreateMod.present();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return CreateMod.present();
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
