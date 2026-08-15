package com.toroidalworld.compat.c2me;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

// Whether C2ME's no-tick view distance is in play. It is a module of its own, and a second chunk loader entirely: it
// walks a spiral around each player and files its tickets straight into the chunk system, so nothing it loads passes
// through the vanilla ticket graph this mod folds. Like the chunk-system rewrite it has no config key — only the
// system property com.ishland.c2me.notickvd.disable turns it off — so presence is the whole condition.
//
// It gates two mixins of its own because both attach to code that exists only when it is loaded: the loader itself,
// and the region cache it injects into ServerAccessibleChunkSending, whose upgradeToThis is an empty Completable
// without it.
public final class C2meNoTickVd {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String LOADER_CLASS = "com/ishland/c2me/notickvd/common/PlayerNoTickLoader.class";

    private static final boolean PRESENT = probe();

    public static boolean present() {
        return PRESENT;
    }

    private static boolean probe() {
        boolean present = C2meNoTickVd.class.getClassLoader().getResource(LOADER_CLASS) != null;
        LOGGER.info("[c2me-compat] gate notickvd_present={}", present);
        return present;
    }

    private C2meNoTickVd() {
    }
}
