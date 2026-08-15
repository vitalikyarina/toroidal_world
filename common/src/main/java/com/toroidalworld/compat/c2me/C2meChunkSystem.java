package com.toroidalworld.compat.c2me;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

// Whether C2ME's chunk-system rewrite is in play. Unlike the aquifer optimisation it carries no config key at all —
// its ModuleEntryPoint declares a hardcoded enabled = true on both loaders — so the module being on the classpath is
// the whole condition, and a resource probe answers it without loading anything.
public final class C2meChunkSystem {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String CHUNK_SYSTEM_CLASS =
            "com/ishland/c2me/rewrites/chunksystem/common/TheChunkSystem.class";

    private static final boolean PRESENT = probe();

    // True when C2ME drives chunk loading and generation, which is when its neighbourhood squares are the ones that
    // have to fold — vanilla's own are then never built.
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
