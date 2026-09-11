package com.toroidalworld.shape;

import com.toroidalworld.shape.torus.CoastFieldLift;

public final class GenerationHookSetup {

    public static void registerAll() {
        CoastFieldLift.register();
    }

    private GenerationHookSetup() {
    }
}
