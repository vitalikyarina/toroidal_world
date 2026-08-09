package com.toroidalworld.client.shape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.toroidalworld.ToroidalWorld;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.WorldDimensions;

// The shapes offered by the World Shape row, and which one is currently picked.
public final class WorldShapes {
    private static final String NORMAL_ID = "normal";
    private static final String NORMAL_LABEL_KEY = "gui.toroidal_world.world_shape.normal";

    // Vanilla's shape: an endless world. It leaves the dimensions exactly as the chosen world type built them, so with
    // no mod registering anything the World Shape row is a single entry that changes nothing.
    public static final WorldShape NORMAL = WorldShape.of(
            ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, NORMAL_ID),
            Component.translatable(NORMAL_LABEL_KEY),
            null,
            (registries, dimensions) -> dimensions);

    private static final List<WorldShape> SHAPES = new ArrayList<>(List.of(NORMAL));

    private static WorldShape selected = NORMAL;

    public static void register(WorldShape shape) {
        SHAPES.add(shape);
    }

    // Normal always leads; the rest follow in a reproducible order, so the cycle button does not depend on mod load order.
    public static List<WorldShape> shapes() {
        return SHAPES.stream()
                .sorted(Comparator.comparing((WorldShape shape) -> shape != NORMAL)
                        .thenComparing(shape -> shape.id().toString()))
                .toList();
    }

    public static WorldShape selected() {
        return selected;
    }

    public static void select(WorldShape shape) {
        selected = shape;
    }

    // Both the selection and each shape's own settings outlive the screen, so without this the next world would be
    // created with whatever the last one was given. Resetting the selection alone is not enough: the settings would
    // sit there unseen and reappear the moment that shape is picked again.
    public static void resetToDefault() {
        selected = NORMAL;
        SHAPES.forEach(shape -> shape.resetSettings().run());
    }

    // Re-create opens the screen from an existing world's stored dimensions. The shape was never stored, so each shape
    // is asked to recognise its own mark in those dimensions; the first that claims them becomes the selection and has
    // already seeded its own settings. Nothing claiming them — a fresh world, or a Normal one — leaves the reset default
    // in place, which is why this runs right after resetToDefault.
    public static void restoreFromExisting(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        for (WorldShape shape : shapes()) {
            if (shape.fromExisting() != null && shape.fromExisting().adopt(registries, dimensions)) {
                selected = shape;
                return;
            }
        }
    }

    public static WorldDimensions applyAtCreation(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        return selected.atCreation().apply(registries, dimensions);
    }

    private WorldShapes() {
    }
}
