package com.toroidalworld.compat.c2me;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModPresence;
import com.toroidalworld.compat.ModSymbol;

public final class C2meNoTickVd {
    static final ModSymbol NO_TICK_LOADER_TACS = new ModSymbol(
            "com/ishland/c2me/notickvd/common/PlayerNoTickLoader", "tacs",
            "Lnet/minecraft/server/level/ChunkMap;");

    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(),
            "com/ishland/c2me/notickvd/common/PlayerNoTickLoader.class",
            "[c2me-compat] gate notickvd_present", NO_TICK_LOADER_TACS);

    public static boolean present() {
        return GATE.present();
    }

    private C2meNoTickVd() {
    }
}
