package com.toroidalworld;

import java.util.Set;

import com.toroidalworld.compat.c2me.C2meAquifer;
import com.toroidalworld.compat.c2me.C2meNativesMath;
import com.toroidalworld.compat.sable.SableMod;

public class ToroidalMixinPlugin extends MixinGatePlugin {
    private static final String AQUIFER_SEAM_MIXIN = "com.toroidalworld.mixin.AquiferSeamMixin";

    private static final String END_ISLAND_MIXIN = "com.toroidalworld.mixin.DensityFunctionsEndIslandMixin";

    private static final String PARROT_MIXIN = "com.toroidalworld.mixin.ParrotMixin";

    private static final String GAME_EVENT_LISTENER_RANGE_MIXIN =
            "com.toroidalworld.mixin.EuclideanGameEventListenerRegistryMixin";

    private static final Set<String> SABLE_CLAIMED_MIXINS = Set.of(PARROT_MIXIN, GAME_EVENT_LISTENER_RANGE_MIXIN);

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (AQUIFER_SEAM_MIXIN.equals(mixinClassName)) {
            return !C2meAquifer.optimizesAquifer();
        }

        if (END_ISLAND_MIXIN.equals(mixinClassName)) {
            return !C2meNativesMath.enabled();
        }

        if (SABLE_CLAIMED_MIXINS.contains(mixinClassName)) {
            return !SableMod.present();
        }

        return true;
    }
}
