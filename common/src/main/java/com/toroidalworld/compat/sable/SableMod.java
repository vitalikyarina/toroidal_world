package com.toroidalworld.compat.sable;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModPresence;
import com.toroidalworld.core.ForeignFrames;

public final class SableMod {
    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(),
            "dev/ryanhcode/sable/Sable.class",
            "[sable-compat] gate sable_present");

    public static boolean present() {
        return GATE.present();
    }

    public static void register() {
        if (present()) {
            ForeignFrames.register(new SableFrames());
        }
    }

    private SableMod() {
    }
}
