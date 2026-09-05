package com.toroidalworld.compat.aeronautics;

import com.mojang.logging.LogUtils;
import com.toroidalworld.compat.ModPresence;
import com.toroidalworld.compat.sable.SableBodyShift;
import com.toroidalworld.compat.sable.SableMod;

public final class AeronauticsMod {
    private static final ModPresence GATE = ModPresence.of(LogUtils.getLogger(),
            "dev/eriksonn/aeronautics/Aeronautics.class",
            "[aeronautics-compat] gate aeronautics_present");

    public static boolean present() {
        return GATE.present();
    }

    public static void register() {
        if (SimulatedMod.present() && SableMod.present()) {
            SableBodyShift.register(RopeSeamFrame::onGroupShifted);
        }
    }

    private AeronauticsMod() {
    }
}
