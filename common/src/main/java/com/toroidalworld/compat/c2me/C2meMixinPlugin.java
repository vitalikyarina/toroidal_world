package com.toroidalworld.compat.c2me;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;

// Gates each C2ME compat mixin on C2ME actually owning the code it attaches to, and brings up MixinSquared before any
// of them is prepared.
//
// A gate per module, because the modules are switched independently: the aquifer fold rides on an option a player can
// turn off (C2meAquifer), while the chunk-system rewrite, the no-tick view distance, the octave loop and the density
// function compiler carry no option and are answered by presence alone. Reading one condition for all of them would
// attach a fold to code that is not there.
//
// The bootstrap call is here rather than left to the library's own platform config: it registers the @MixinSquared
// selector, and a selector that is not registered does not fail — the injection simply matches nothing, which for a
// fold means a seam that splits with nothing in the log. init() is idempotent, so making sure costs one call.
public class C2meMixinPlugin implements IMixinConfigPlugin {
    private static final String AQUIFER_MIXIN = "AquiferSeamMixin";

    private static final String OCTAVE_NOISE_MIXIN = "PerlinNoiseMixin";

    private static final String[] NO_TICK_VD_MIXINS = {
            "PlayerNoTickLoaderMixin",
            "ServerAccessibleChunkSendingMixin"
    };

    private static final String[] DFC_MIXINS = {
            "McToAstMixin",
            "BytecodeGenRegistryMixin",
            "DotGenRegistryMixin"
    };

    @Override
    public void onLoad(String mixinPackage) {
        MixinSquaredBootstrap.init();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(AQUIFER_MIXIN)) {
            return C2meAquifer.optimizesAquifer();
        }

        if (mixinClassName.endsWith(OCTAVE_NOISE_MIXIN)) {
            return C2meOctaveNoise.present();
        }

        for (String noTickVdMixin : NO_TICK_VD_MIXINS) {
            if (mixinClassName.endsWith(noTickVdMixin)) {
                return C2meNoTickVd.present();
            }
        }

        for (String dfcMixin : DFC_MIXINS) {
            if (mixinClassName.endsWith(dfcMixin)) {
                return C2meDfc.present();
            }
        }

        return C2meChunkSystem.present();
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
