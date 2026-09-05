package com.toroidalworld.compat.aeronautics;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModPresence;

public final class SimulatedMod {
    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(),
            "dev/simulated_team/simulated/Simulated.class",
            "[aeronautics-compat] gate simulated_present");

    public static boolean present() {
        return GATE.present();
    }

    private SimulatedMod() {
    }
}
