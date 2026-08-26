package com.toroidalworld.client.shape.torus;

import com.toroidalworld.client.shape.ShapeCustomizers;
import com.toroidalworld.shape.torus.TorusSettings;
import com.toroidalworld.shape.torus.TorusShape;

import net.minecraft.client.gui.screens.Screen;

public final class TorusShapeSetup {

    public static void register() {
        ShapeCustomizers.register(TorusShape.ID, TorusShapeSetup::createScreen);
    }

    private static Screen createScreen(Screen parent) {
        TorusSettings settings = TorusShape.settings();
        return new TorusSettingsScreen(parent, settings.overworld(), settings.netherScale(), settings.end(),
                (chosen, chosenScale, chosenEnd) ->
                        TorusShape.settings(new TorusSettings(chosen, chosenScale, chosenEnd)));
    }

    private TorusShapeSetup() {
    }
}
