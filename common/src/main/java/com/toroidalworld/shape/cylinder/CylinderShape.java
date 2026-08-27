package com.toroidalworld.shape.cylinder;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.client.shape.cylinder.CylinderShapeSetup;
import com.toroidalworld.platform.Platforms;
import com.toroidalworld.shape.WorldShape;
import com.toroidalworld.shape.WorldShapes;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class CylinderShape {
    private static final String CYLINDER_ID = "cylinder";
    private static final String CYLINDER_LABEL_KEY = "gui.toroidal_world.world_shape.cylinder";
    private static final String CYLINDER_HINT_KEY = "gui.toroidal_world.world_shape.cylinder.hint";

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, CYLINDER_ID);

    private static CylinderSettings settings = CylinderSettings.DEFAULT;

    public static void register() {
        WorldShapes.register(WorldShape.of(
                ID,
                Component.translatable(CYLINDER_LABEL_KEY),
                Component.translatable(CYLINDER_HINT_KEY),
                CylinderShape::applyAtCreation,
                CylinderShape::resetSettings,
                CylinderShape::restoreFromExisting));

        if (Platforms.get().isClient()) {
            CylinderShapeSetup.register();
        }
    }

    public static CylinderSettings settings() {
        return settings;
    }

    public static void settings(CylinderSettings chosen) {
        settings = chosen;
    }

    private static void resetSettings() {
        settings = CylinderSettings.DEFAULT;
    }

    private static boolean restoreFromExisting(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        CylinderSettings restored = CylinderDimensions.read(dimensions);
        if (restored == null) {
            return false;
        }

        settings = restored;
        return true;
    }

    private static WorldDimensions applyAtCreation(RegistryAccess.Frozen registries, WorldDimensions dimensions) {
        return CylinderDimensions.apply(dimensions, settings);
    }

    private CylinderShape() {
    }
}
