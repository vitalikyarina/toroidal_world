package com.toroidalworld.compat.aeronautics;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.sable.SableBodyShift;
import com.toroidalworld.compat.sable.SableMod;

public final class AeronauticsMod {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String AERONAUTICS_CLASS = "dev/eriksonn/aeronautics/Aeronautics.class";

    private static final boolean PRESENT = probe();

    public static boolean present() {
        return PRESENT;
    }

    public static void register() {
        if (SimulatedMod.present() && SableMod.present()) {
            SableBodyShift.register(RopeSeamFrame::onGroupShifted);
        }
    }

    private static boolean probe() {
        boolean present = AeronauticsMod.class.getClassLoader().getResource(AERONAUTICS_CLASS) != null;
        LOGGER.info("[aeronautics-compat] gate aeronautics_present={}", present);
        return present;
    }

    private AeronauticsMod() {
    }
}
