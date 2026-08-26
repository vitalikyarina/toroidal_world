package com.toroidalworld.client.shape.cylinder;

import com.toroidalworld.client.shape.ShapeCustomizers;
import com.toroidalworld.shape.cylinder.CylinderShape;

import net.minecraft.client.gui.screens.Screen;

public final class CylinderShapeSetup {

    public static void register() {
        ShapeCustomizers.register(CylinderShape.ID, CylinderShapeSetup::createScreen);
    }

    private static Screen createScreen(Screen parent) {
        return new CylinderSettingsScreen(parent, CylinderShape.settings(), CylinderShape::settings);
    }

    private CylinderShapeSetup() {
    }
}
