package com.toroidalworld.compat.create;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.MixinGatePlugin;
import com.toroidalworld.compat.sable.SableMod;

public class CreateMixinPlugin extends MixinGatePlugin {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String BLUEPRINT_REACH_MIXIN =
            "com.toroidalworld.compat.create.mixin.BlueprintReachMixin";

    @Override
    public void onLoad(String mixinPackage) {
        // Evaluated here rather than left to the first shouldApplyMixin call: the gate logs the answer as it probes,
        // and with the config carrying no mixin yet nothing would ever query it — so the one line saying whether
        // Create was seen would be missing exactly on the run that has to prove the module is inert without it.
        CreateMod.present();
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!CreateMod.present()) {
            return false;
        }

        if (BLUEPRINT_REACH_MIXIN.equals(mixinClassName)) {
            // Mixin refuses any injection into a method another mixin merged, so applying this beside Sable's
            // canPlayerUse overwrite drops the whole class and takes the two unrelated blueprint folds with it.
            boolean sablePresent = SableMod.present();
            LOGGER.info("[create-compat] gate blueprint_reach sable_present={} applied={}", sablePresent,
                    !sablePresent);
            return !sablePresent;
        }

        return true;
    }
}
