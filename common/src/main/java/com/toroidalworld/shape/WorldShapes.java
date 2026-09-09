package com.toroidalworld.shape;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.core.StartupRegistry;
import com.toroidalworld.engine.gen.ShapedDimensions;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class WorldShapes {
    private static final String NORMAL_PATH = "normal";

    private static final Identifier NORMAL_ID = Identifier.fromNamespaceAndPath(ToroidalWorld.MODID, NORMAL_PATH);

    public static final WorldShape NORMAL = WorldShape.of(
            NORMAL_ID,
            WorldShape.label(NORMAL_ID),
            WorldShape.hint(NORMAL_ID),
            (registries, dimensions) -> dimensions);

    private static final StartupRegistry<Identifier, WorldShape> SHAPES = new StartupRegistry<>("World shapes");

    private static WorldShape selected = NORMAL;

    public static void register(WorldShape shape) {
        SHAPES.register(shape.id(), shape);
    }

    public static List<WorldShape> shapes() {
        return Stream.concat(
                Stream.of(NORMAL),
                SHAPES.entries().values().stream()
                        .sorted(Comparator.comparing((WorldShape shape) -> shape.id().toString())))
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
        shapes().forEach(shape -> shape.resetSettings().run());
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
