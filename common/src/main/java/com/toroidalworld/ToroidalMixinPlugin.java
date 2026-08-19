package com.toroidalworld;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.toroidalworld.compat.c2me.C2meAquifer;
import com.toroidalworld.compat.c2me.C2meNativesMath;

public class ToroidalMixinPlugin implements IMixinConfigPlugin {
    private static final String AQUIFER_SEAM_MIXIN = "com.toroidalworld.mixin.AquiferSeamMixin";

    private static final String END_ISLAND_MIXIN = "com.toroidalworld.mixin.DensityFunctionsEndIslandMixin";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (AQUIFER_SEAM_MIXIN.equals(mixinClassName)) {
            return !C2meAquifer.optimizesAquifer();
        }

        if (END_ISLAND_MIXIN.equals(mixinClassName)) {
            return !C2meNativesMath.enabled();
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
