package com.toroidalworld;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.toroidalworld.compat.c2me.C2meAquifer;

// The main config's mixins are unconditional but for one case: where another mod has taken over the vanilla code a
// fold attaches to, this mod's copy of that fold steps aside and the compat module's copy takes over. Both sides read
// the same condition, so exactly one is ever live.
//
// It is a switch, never a silence. Dropping the failing injection instead (require = 0) would leave the seam unfolded
// with nothing in the log — the failure this mod is least able to see. AquiferSeamMixin is the first such pair: with
// C2ME's aquifer optimisation on, its @Overwrite of computeSubstance removes the call that fold wraps, and the mixin
// does not merely miss — it fails to apply, kills the worldgen task on a C2ME worker thread, and hangs the server
// thread on a chunk that never arrives.
public class ToroidalMixinPlugin implements IMixinConfigPlugin {
    private static final String AQUIFER_SEAM_MIXIN = "com.toroidalworld.mixin.AquiferSeamMixin";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (AQUIFER_SEAM_MIXIN.equals(mixinClassName)) {
            return !C2meAquifer.optimizesAquifer();
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
