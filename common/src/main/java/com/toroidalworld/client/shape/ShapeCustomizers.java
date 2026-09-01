package com.toroidalworld.client.shape;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.shape.WorldShape;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

public final class ShapeCustomizers {
    @FunctionalInterface
    public interface Customizer {
        Screen createScreen(Screen parent);
    }

    private static final Map<Identifier, Customizer> CUSTOMIZERS = new HashMap<>();

    public static void register(Identifier shapeId, Customizer customizer) {
        CUSTOMIZERS.put(shapeId, customizer);
    }

    public static @Nullable Customizer of(WorldShape shape) {
        return CUSTOMIZERS.get(shape.id());
    }

    private ShapeCustomizers() {
    }
}
