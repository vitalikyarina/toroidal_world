package com.toroidalworld.compat.c2me;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

public final class C2meChunkSystem {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CHUNK_SYSTEM_CLASS =
            "com/ishland/c2me/rewrites/chunksystem/common/TheChunkSystem.class";

    private static final boolean PRESENT = probe();

    public static boolean present() {
        return PRESENT;
    }

    private static boolean probe() {
        boolean present = C2meChunkSystem.class.getClassLoader().getResource(CHUNK_SYSTEM_CLASS) != null;
        LOGGER.info("[c2me-compat] gate chunk_system_present={}", present);
        return present;
    }

    private C2meChunkSystem() {
    }
}
