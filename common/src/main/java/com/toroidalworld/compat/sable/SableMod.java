package com.toroidalworld.compat.sable;

import org.slf4j.Logger;

import com.toroidalworld.core.ForeignFrames;
import com.mojang.logging.LogUtils;

public final class SableMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String SABLE_CLASS = "dev/ryanhcode/sable/Sable.class";

    private static final boolean PRESENT = probe();

    public static boolean present() {
        return PRESENT;
    }

    public static void register() {
        if (PRESENT) {
            ForeignFrames.register(new SableFrames());
        }
    }

    private static boolean probe() {
        boolean present = SableMod.class.getClassLoader().getResource(SABLE_CLASS) != null;
        LOGGER.info("[sable-compat] gate sable_present={}", present);
        return present;
    }

    private SableMod() {
    }
}
