package com.toroidalworld.shape;

import com.toroidalworld.shape.cylinder.CylinderShape;
import com.toroidalworld.shape.torus.TorusShape;

public final class WorldShapeSetup {

    public static void registerAll() {
        TorusShape.register();
        CylinderShape.register();
    }

    private WorldShapeSetup() {
    }
}
