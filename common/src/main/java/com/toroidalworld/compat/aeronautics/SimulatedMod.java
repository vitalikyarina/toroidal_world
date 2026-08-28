package com.toroidalworld.compat.aeronautics;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

public final class SimulatedMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String SIMULATED_CLASS = "dev/simulated_team/simulated/Simulated.class";

    private static final boolean PRESENT = probe();

    public static boolean present() {
        return PRESENT;
    }

    private static boolean probe() {
        boolean present = SimulatedMod.class.getClassLoader().getResource(SIMULATED_CLASS) != null;
        LOGGER.info("[aeronautics-compat] gate simulated_present={}", present);
        return present;
    }

    private SimulatedMod() {
    }
}
