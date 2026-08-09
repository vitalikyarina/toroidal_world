package com.toroidalworld.client.shape;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.WorldDimensions;

// What shape a world has — as opposed to its **type**, which says how the terrain is generated. The two are orthogonal:
// a shape composes with any type, which is why this is its own row on the World tab rather than another entry in the
// type list.
//
// A shape reaches the world through {@link AtCreation}, applied once while the world is being created. That timing is
// the whole trick: choosing a world type rebuilds the dimensions from its preset, so a shape applied any earlier would
// be silently thrown away — and vanilla stores a world's generators, never the choices it was created from, so the
// shape has to leave its mark inside them.
public record WorldShape(ResourceLocation id, Component label, @Nullable Customizer customizer, AtCreation atCreation,
        Runnable resetSettings, @Nullable FromExisting fromExisting) {
    // Rebuilds the dimensions the chosen world type produced. Returning them unchanged means "leave this world alone".
    @FunctionalInterface
    public interface AtCreation {
        WorldDimensions apply(RegistryAccess.Frozen registries, WorldDimensions dimensions);
    }

    // Re-create opens the screen from an existing world's stored dimensions, but the shape was never stored — only the
    // generator it left behind. A shape that can recognise its own mark in those dimensions returns true and seeds its
    // settings from them; the screen then opens on this world's real shape instead of the default. Returning false means
    // "not mine" — a fresh world, or one shaped by a different shape.
    @FunctionalInterface
    public interface FromExisting {
        boolean adopt(RegistryAccess.Frozen registries, WorldDimensions dimensions);
    }

    // The screen behind the shape's Customize button. A shape without one leaves the button inactive, exactly as a
    // world type without a preset editor does.
    @FunctionalInterface
    public interface Customizer {
        Screen createScreen(Screen parent);
    }

    public static WorldShape of(ResourceLocation id, Component label, @Nullable Customizer customizer, AtCreation atCreation) {
        return new WorldShape(id, label, customizer, atCreation, () -> {
        }, null);
    }

    // A shape whose customizer writes settings that outlive the screen must say how to put them back, or the next world
    // would be created with whatever the last one was given. A shape with nothing to remember uses the overload above.
    public static WorldShape of(ResourceLocation id, Component label, @Nullable Customizer customizer, AtCreation atCreation,
            Runnable resetSettings) {
        return new WorldShape(id, label, customizer, atCreation, resetSettings, null);
    }

    // Adds re-create support: the shape also knows how to recognise itself in an existing world's dimensions and seed
    // its settings from them. Without this the shape simply never claims a re-created world, which reads as the default.
    public static WorldShape of(ResourceLocation id, Component label, @Nullable Customizer customizer, AtCreation atCreation,
            Runnable resetSettings, FromExisting fromExisting) {
        return new WorldShape(id, label, customizer, atCreation, resetSettings, fromExisting);
    }
}
