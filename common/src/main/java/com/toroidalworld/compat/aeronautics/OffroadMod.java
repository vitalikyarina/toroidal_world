package com.toroidalworld.compat.aeronautics;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModPresence;

public final class OffroadMod {
    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(),
            "dev/ryanhcode/offroad/Offroad.class",
            "[aeronautics-compat] gate offroad_present");

    public static boolean present() {
        return GATE.present();
    }

    private OffroadMod() {
    }
}
