package com.toroidalworld.compat.aeronautics;

import java.util.function.BooleanSupplier;

enum BundleMod {
    AERONAUTICS("dev.eriksonn.aeronautics.", AeronauticsMod::present),
    SIMULATED("dev.simulated_team.simulated.", SimulatedMod::present),
    OFFROAD("dev.ryanhcode.offroad.", OffroadMod::present);

    private final String targetPackage;
    private final BooleanSupplier gate;

    BundleMod(String targetPackage, BooleanSupplier gate) {
        this.targetPackage = targetPackage;
        this.gate = gate;
    }

    static BundleMod owning(String targetClassName) {
        for (BundleMod mod : values()) {
            if (targetClassName.startsWith(mod.targetPackage)) {
                return mod;
            }
        }

        return null;
    }

    boolean present() {
        return this.gate.getAsBoolean();
    }
}
