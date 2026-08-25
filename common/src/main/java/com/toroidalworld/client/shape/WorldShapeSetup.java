package com.toroidalworld.client.shape;

import com.toroidalworld.client.shape.torus.TorusShapeSetup;

public final class WorldShapeSetup {

    public static void registerAll() {
        TorusShapeSetup.register();
    }

    private WorldShapeSetup() {
    }
}
