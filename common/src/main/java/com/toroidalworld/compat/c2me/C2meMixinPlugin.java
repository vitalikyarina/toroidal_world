package com.toroidalworld.compat.c2me;

import org.spongepowered.asm.mixin.Mixins;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;
import com.toroidalworld.MixinGatePlugin;

public class C2meMixinPlugin extends MixinGatePlugin {
    private static final String AQUIFER_MIXIN = "AquiferSeamMixin";

    private static final String END_ISLAND_MIXIN = "EndIslandSeamMixin";

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
        Mixins.registerErrorHandlerClass(C2meMixinErrorHandler.class.getName());
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(AQUIFER_MIXIN)) {
            return C2meAquifer.optimizesAquifer();
        }

        if (mixinClassName.endsWith(END_ISLAND_MIXIN)) {
            return C2meNativesMath.enabled();
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
}
