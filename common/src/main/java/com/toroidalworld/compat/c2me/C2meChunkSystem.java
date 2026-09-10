package com.toroidalworld.compat.c2me;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModPresence;
import com.toroidalworld.compat.ModSymbol;

public final class C2meChunkSystem {
    static final ModSymbol CHUNK_SYSTEM_TACS = new ModSymbol(
            "com/ishland/c2me/rewrites/chunksystem/common/TheChunkSystem", "tacs",
            "Lnet/minecraft/server/level/ChunkMap;");

    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(),
            "com/ishland/c2me/rewrites/chunksystem/common/TheChunkSystem.class",
            "[c2me-compat] gate chunk_system_present", CHUNK_SYSTEM_TACS);

    public static boolean present() {
        return GATE.present();
    }

    private C2meChunkSystem() {
    }
}
