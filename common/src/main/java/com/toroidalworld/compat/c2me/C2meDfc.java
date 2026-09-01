package com.toroidalworld.compat.c2me;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

public final class C2meDfc {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String ROUTER_MIXIN_CLASS = "com/ishland/c2me/opts/dfc/mixin/MixinNoiseConfig.class";

    private static final boolean PRESENT = probe();

    public static boolean present() {
        return PRESENT;
    }

    private static boolean probe() {
        boolean present = C2meDfc.class.getClassLoader().getResource(ROUTER_MIXIN_CLASS) != null;
        LOGGER.info("[c2me-compat] gate dfc_present={}", present);
        return present;
    }

    private C2meDfc() {
    }
}
