package com.toroidalworld.compat.c2me;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModPresence;

public final class C2meChunkSystem {
    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(),
            "com/ishland/c2me/rewrites/chunksystem/common/TheChunkSystem.class",
            "[c2me-compat] gate chunk_system_present");

    public static boolean present() {
        return GATE.present();
    }

    private C2meChunkSystem() {
    }
}
