package com.toroidalworld.shape.torus;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.client.shape.torus.TorusShapeSetup;
import com.toroidalworld.api.v1.shape.ShapeModule;

import net.minecraft.resources.ResourceLocation;

public final class TorusShape {
    private static final String TORUS_PATH = "toroidal";

    public static final ShapeModule<TorusSettings> MODULE = ShapeModule.of(
            ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, TORUS_PATH),
            TorusSettings.DEFAULT,
            TorusDimensions::apply,
            TorusDimensions::read);

    public static void register(boolean client) {
        MODULE.register();

        if (client) {
            TorusShapeSetup.register();
        }
    }

    private TorusShape() {
    }
}
