package com.toroidalworld.shape;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.WorldDimensions;

public record WorldShape(ResourceLocation id, Component label, Component hint, AtCreation atCreation,
        Runnable resetSettings, @Nullable FromExisting fromExisting) {
    private static final String KEY_PREFIX = "gui.";
    private static final String KEY_INFIX = ".world_shape.";
    private static final String HINT_KEY_SUFFIX = ".hint";

    @FunctionalInterface
    public interface AtCreation {
        WorldDimensions apply(RegistryAccess.Frozen registries, WorldDimensions dimensions);
    }

    @FunctionalInterface
    public interface FromExisting {
        boolean adopt(RegistryAccess.Frozen registries, WorldDimensions dimensions);
    }

    public static WorldShape of(ResourceLocation id, Component label, Component hint, AtCreation atCreation) {
        return new WorldShape(id, label, hint, atCreation, () -> {
        }, null);
    }

    public static WorldShape of(ResourceLocation id, Component label, Component hint, AtCreation atCreation,
            Runnable resetSettings, FromExisting fromExisting) {
        return new WorldShape(id, label, hint, atCreation, resetSettings, fromExisting);
    }

    public static Component label(ResourceLocation id) {
        return Component.translatable(labelKey(id));
    }

    public static Component hint(ResourceLocation id) {
        return Component.translatable(labelKey(id) + HINT_KEY_SUFFIX);
    }

    private static String labelKey(ResourceLocation id) {
        return KEY_PREFIX + id.getNamespace() + KEY_INFIX + id.getPath();
    }
}
