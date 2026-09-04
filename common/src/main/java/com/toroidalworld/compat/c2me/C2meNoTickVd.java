package com.toroidalworld.compat.c2me;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModPresence;

public final class C2meNoTickVd {
    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(),
            "com/ishland/c2me/notickvd/common/PlayerNoTickLoader.class",
            "[c2me-compat] gate notickvd_present");

    public static boolean present() {
        return GATE.present();
    }

    private C2meNoTickVd() {
    }
}
