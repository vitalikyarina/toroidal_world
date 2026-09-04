package com.toroidalworld;

import com.toroidalworld.compat.c2me.C2meAquifer;
import com.toroidalworld.compat.c2me.C2meNativesMath;

public class ToroidalMixinPlugin extends MixinGatePlugin {
    private static final String AQUIFER_SEAM_MIXIN = "com.toroidalworld.mixin.AquiferSeamMixin";

    private static final String END_ISLAND_MIXIN = "com.toroidalworld.mixin.DensityFunctionsEndIslandMixin";

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
}
