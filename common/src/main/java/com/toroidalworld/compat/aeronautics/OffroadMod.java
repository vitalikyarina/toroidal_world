package com.toroidalworld.compat.aeronautics;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

public final class OffroadMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String OFFROAD_CLASS = "dev/ryanhcode/offroad/Offroad.class";

    private static final boolean PRESENT = probe();

    public static boolean present() {
        return PRESENT;
    }

    private static boolean probe() {
        boolean present = OffroadMod.class.getClassLoader().getResource(OFFROAD_CLASS) != null;
        LOGGER.info("[aeronautics-compat] gate offroad_present={}", present);
        return present;
    }

    private OffroadMod() {
    }
}
