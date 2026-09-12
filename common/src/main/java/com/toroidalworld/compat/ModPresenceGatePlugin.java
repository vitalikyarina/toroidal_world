package com.toroidalworld.compat;

import com.toroidalworld.MixinGatePlugin;

public abstract class ModPresenceGatePlugin extends MixinGatePlugin {
    private final ModPresence presence;

    protected ModPresenceGatePlugin(ModPresence presence) {
        this.presence = presence;
    }

    @Override
    public void onLoad(String mixinPackage) {
        this.presence.present();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return this.presence.present();
    }
}
