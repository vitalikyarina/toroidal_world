package com.toroidalworld.shape;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.WorldDimensions;

public record WorldShape(Identifier id, Component label, Component hint, AtCreation atCreation,
        Runnable resetSettings, @Nullable FromExisting fromExisting) {
    @FunctionalInterface
    public interface AtCreation {
        WorldDimensions apply(RegistryAccess.Frozen registries, WorldDimensions dimensions);
    }

    @FunctionalInterface
    public interface FromExisting {
        boolean adopt(RegistryAccess.Frozen registries, WorldDimensions dimensions);
    }

    public static WorldShape of(Identifier id, Component label, Component hint, AtCreation atCreation) {
        return new WorldShape(id, label, hint, atCreation, () -> {
        }, null);
    }

    public static WorldShape of(Identifier id, Component label, Component hint, AtCreation atCreation,
            Runnable resetSettings, FromExisting fromExisting) {
        return new WorldShape(id, label, hint, atCreation, resetSettings, fromExisting);
    }
}
