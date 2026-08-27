package com.toroidalworld.client.shape;

import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.shape.WorldShape;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

public final class ShapeCustomizers {
    @FunctionalInterface
    public interface Customizer {
        Screen createScreen(Screen parent);
    }

    private static final Map<ResourceLocation, Customizer> CUSTOMIZERS = new HashMap<>();

    public static void register(ResourceLocation shapeId, Customizer customizer) {
        CUSTOMIZERS.put(shapeId, customizer);
    }

    public static @Nullable Customizer of(WorldShape shape) {
        return CUSTOMIZERS.get(shape.id());
    }

    private ShapeCustomizers() {
    }
}
