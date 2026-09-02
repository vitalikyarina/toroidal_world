package com.toroidalworld.shape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.gen.ShapedDimensions;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class WorldShapes {
    private static final String NORMAL_ID = "normal";
    private static final String NORMAL_LABEL_KEY = "gui.toroidal_world.world_shape.normal";
    private static final String NORMAL_HINT_KEY = "gui.toroidal_world.world_shape.normal.hint";

    public static final WorldShape NORMAL = WorldShape.of(
            ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, NORMAL_ID),
            Component.translatable(NORMAL_LABEL_KEY),
            Component.translatable(NORMAL_HINT_KEY),
            (registries, dimensions) -> dimensions);

    private static final List<WorldShape> SHAPES = new ArrayList<>(List.of(NORMAL));

    private static WorldShape selected = NORMAL;

    public static void register(WorldShape shape) {
        SHAPES.add(shape);
    }

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

    public static void resetToDefault() {
        selected = NORMAL;
        SHAPES.forEach(shape -> shape.resetSettings().run());
    }

    public static void restoreFromExisting(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        for (WorldShape shape : shapes()) {
            if (shape.fromExisting() != null && shape.fromExisting().adopt(registries, dimensions)) {
                selected = shape;
                return;
            }
        }
    }

    public static WorldDimensions applyAtCreation(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        return selected.atCreation().apply(registries, ShapedDimensions.stripShapes(dimensions));
    }

    private WorldShapes() {
    }
}
