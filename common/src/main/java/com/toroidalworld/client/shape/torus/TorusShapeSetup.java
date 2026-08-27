package com.toroidalworld.client.shape.torus;

import com.toroidalworld.ToroidalWorld;
import com.toroidalworld.client.shape.WorldShape;
import com.toroidalworld.client.shape.WorldShapes;
import com.toroidalworld.gen.WorldLoopGenerators;
import com.toroidalworld.shape.torus.TorusDimensions;
import com.toroidalworld.shape.torus.TorusSettings;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.WorldDimensions;

public final class TorusShapeSetup {
    private static final String TOROIDAL_LABEL_KEY = "gui.toroidal_world.world_shape.toroidal";

    private static TorusSettings settings = TorusSettings.DEFAULT;

    public static void register() {
        WorldShapes.register(WorldShape.of(
                ResourceLocation.fromNamespaceAndPath(ToroidalWorld.MODID, WorldLoopGenerators.TOROIDAL_ID),
                Component.translatable(TOROIDAL_LABEL_KEY),
                parent -> new TorusSettingsScreen(parent, settings.overworld(), settings.netherScale(),
                        settings.end(),
                        (chosen, chosenScale, chosenEnd) ->
                                settings = new TorusSettings(chosen, chosenScale, chosenEnd)),
                TorusShapeSetup::applyAtCreation,
                TorusShapeSetup::resetSettings,
                TorusShapeSetup::restoreFromExisting));
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

    private TorusShapeSetup() {
    }
}
