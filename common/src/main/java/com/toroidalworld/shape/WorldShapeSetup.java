package com.toroidalworld.shape;

import com.toroidalworld.shape.torus.TorusShape;

public final class WorldShapeSetup {

    public static void registerAll() {
        TorusShape.register();
    }

    private WorldShapeSetup() {
    }
}
