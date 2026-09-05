package com.toroidalworld.compat.sable;

import com.toroidalworld.MixinGatePlugin;

public class SableMixinPlugin extends MixinGatePlugin {
    @Override
    public void onLoad(String mixinPackage) {
        SableMod.present();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return SableMod.present();
    }
}
