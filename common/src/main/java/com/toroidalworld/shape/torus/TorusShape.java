package com.toroidalworld.shape.torus;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.client.shape.torus.TorusShapeSetup;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.shape.WorldShape;
import com.toroidalworld.shape.WorldShapes;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class TorusShape {
    private static final String TORUS_ID = "toroidal";
    private static final String TORUS_LABEL_KEY = "gui.toroidal_world.world_shape.toroidal";

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, TORUS_ID);

    private static TorusSettings settings = TorusSettings.DEFAULT;

    public static void register() {
        WorldShapes.register(WorldShape.of(
                ID,
                Component.translatable(TORUS_LABEL_KEY),
                TorusShape::applyAtCreation,
                TorusShape::resetSettings,
                TorusShape::restoreFromExisting));

        if (Platforms.get().isClient()) {
            TorusShapeSetup.register();
        }
    }

    public static TorusSettings settings() {
        return settings;
    }

    public static void settings(TorusSettings chosen) {
        settings = chosen;
    }

    private static void resetSettings() {
        settings = TorusSettings.DEFAULT;
    }

    private static boolean restoreFromExisting(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        TorusSettings restored = TorusDimensions.read(dimensions);
        if (restored == null) {
            return false;
        }

        settings = restored;
        return true;
    }

    private static WorldDimensions applyAtCreation(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        return TorusDimensions.apply(dimensions, settings);
    }

    private TorusShape() {
    }
}
