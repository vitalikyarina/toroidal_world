package com.toroidalworld.shape;

import com.toroidalworld.platform.Platforms;
import com.toroidalworld.shape.cylinder.CylinderShape;
import com.toroidalworld.shape.torus.TorusShape;

public final class WorldShapeSetup {

    public static void registerAll() {
        boolean client = Platforms.get().isClient();
        TorusShape.register(client);
        CylinderShape.register(client);
    }

    private WorldShapeSetup() {
    }
}
