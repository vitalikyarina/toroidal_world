package com.toroidalworld.shape.cylinder;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.client.shape.cylinder.CylinderShapeSetup;
import com.toroidalworld.shape.ShapeModule;

import net.minecraft.resources.ResourceLocation;

public final class CylinderShape {
    private static final String CYLINDER_PATH = "cylinder";

    public static final ShapeModule<CylinderSettings> MODULE = ShapeModule.of(
            ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, CYLINDER_PATH),
            CylinderSettings.DEFAULT,
            CylinderDimensions::apply,
            CylinderDimensions::read);

    public static void register(boolean client) {
        MODULE.register();

        if (client) {
            CylinderShapeSetup.register();
        }
    }

    private CylinderShape() {
    }
}
