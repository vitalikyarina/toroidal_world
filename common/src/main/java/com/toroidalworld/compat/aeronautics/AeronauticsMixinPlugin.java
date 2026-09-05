package com.toroidalworld.compat.aeronautics;

import com.toroidalworld.MixinGatePlugin;

public class AeronauticsMixinPlugin extends MixinGatePlugin {
    private static final String OFFROAD_MIXIN = "MultiMiningSyncAccessor";

    @Override
    public void onLoad(String mixinPackage) {
        SimulatedMod.present();
        OffroadMod.present();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return mixinClassName.endsWith(OFFROAD_MIXIN) ? OffroadMod.present() : SimulatedMod.present();
    }
}
