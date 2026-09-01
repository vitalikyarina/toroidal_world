package com.toroidalworld.compat.c2me;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

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
