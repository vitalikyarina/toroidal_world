package com.toroidalworld.client.shape;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.WorldDimensions;

public record WorldShape(ResourceLocation id, Component label, @Nullable Customizer customizer, AtCreation atCreation,
        Runnable resetSettings, @Nullable FromExisting fromExisting) {
    @FunctionalInterface
    public interface AtCreation {
        WorldDimensions apply(RegistryAccess.Frozen registries, WorldDimensions dimensions);
    }

    @FunctionalInterface
    public interface FromExisting {
        boolean adopt(RegistryAccess.Frozen registries, WorldDimensions dimensions);
    }

    @FunctionalInterface
    public interface Customizer {
        Screen createScreen(Screen parent);
    }

    public static WorldShape of(ResourceLocation id, Component label, @Nullable Customizer customizer, AtCreation atCreation) {
        return new WorldShape(id, label, customizer, atCreation, () -> {
        }, null);
    }

    public static WorldShape of(ResourceLocation id, Component label, @Nullable Customizer customizer, AtCreation atCreation,
            Runnable resetSettings) {
        return new WorldShape(id, label, customizer, atCreation, resetSettings, null);
    }

    public static WorldShape of(ResourceLocation id, Component label, @Nullable Customizer customizer, AtCreation atCreation,
            Runnable resetSettings, FromExisting fromExisting) {
        return new WorldShape(id, label, customizer, atCreation, resetSettings, fromExisting);
    }
}
