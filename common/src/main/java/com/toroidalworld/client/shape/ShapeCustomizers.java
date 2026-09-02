package com.toroidalworld.client.shape;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.registry.StartupRegistry;
import com.toroidalworld.shape.WorldShape;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

public final class ShapeCustomizers {
    @FunctionalInterface
    public interface Customizer {
        Screen createScreen(Screen parent);
    }

    private static final StartupRegistry<ResourceLocation, Customizer> CUSTOMIZERS =
            new StartupRegistry<>("Shape customizers");

    public static void register(ResourceLocation shapeId, Customizer customizer) {
        CUSTOMIZERS.register(shapeId, customizer);
    }

    public static @Nullable Customizer of(WorldShape shape) {
        return CUSTOMIZERS.entries().get(shape.id());
    }

    private ShapeCustomizers() {
    }
}
