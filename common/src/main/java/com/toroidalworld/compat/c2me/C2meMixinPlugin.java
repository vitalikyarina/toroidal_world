package com.toroidalworld.compat.c2me;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;

// Gates the C2ME compat mixins on C2ME actually owning the code they attach to (C2meAquifer), and brings up
// MixinSquared before any of them is prepared.
//
// The bootstrap call is here rather than left to the library's own platform config: it registers the @MixinSquared
// selector, and a selector that is not registered does not fail — the injection simply matches nothing, which for a
// fold means a seam that splits with nothing in the log. init() is idempotent, so making sure costs one call.
public class C2meMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
        MixinSquaredBootstrap.init();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return C2meAquifer.optimizesAquifer();
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
