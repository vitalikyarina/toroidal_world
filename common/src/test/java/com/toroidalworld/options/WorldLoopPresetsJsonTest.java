package com.toroidalworld.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.IntFunction;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.toroidalworld.shape.cylinder.CylinderSettings;

import net.minecraft.core.Direction;

class WorldLoopPresetsJsonTest {
    private static final String PRESET_RESOURCE_DIR = "/data/toroidal_world/worldgen/world_preset/";
    private static final String LOOPED_GENERATOR_ID = "toroidal_world:toroidal";
    private static final String CYLINDER_PRESET_PREFIX = "cylinder_";
    private static final String CLIMATE_COMPRESSION_KEY = "climate_compression";
    private static final String GUARANTEED_LAND_KEY = "guaranteed_land";

    private static final boolean TORUS_CLIMATE_COMPRESSION = false;

    private static final boolean TORUS_GUARANTEED_LAND = true;

    @Test
    void everyPresetShipsATorusWorldPresetMatchingItsConfiguration() throws IOException {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            assertPreset(preset.id(), preset, WorldLoopBounds::ofWidth, TORUS_CLIMATE_COMPRESSION);
        }
    }

    @Test
    void everyPresetShipsACylinderWorldPresetOnTheDefaultAxis() throws IOException {
        Direction.Axis axis = CylinderSettings.DEFAULT.axis();
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            assertPreset(CYLINDER_PRESET_PREFIX + preset.id(), preset, width -> WorldLoopBounds.ofWidth(axis, width),
                    null);
        }
    }

    private static void assertPreset(String presetId, WorldLoopPresets preset, IntFunction<WorldLoopBounds> boundsOfWidth,
            @Nullable Boolean climateCompression) throws IOException {
        JsonObject dimensions = readPresetJson(presetId).getAsJsonObject("dimensions");
        assertNotNull(dimensions, presetId + ": no dimensions object");
        assertEquals(3, dimensions.size(), presetId + ": expected exactly the three vanilla dimensions");

        assertDimension(presetId, dimensions, "minecraft:overworld", "minecraft:overworld",
                boundsOfWidth.apply(preset.chunkWidth()), climateCompression);
        assertDimension(presetId, dimensions, "minecraft:the_nether", "minecraft:nether",
                boundsOfWidth.apply(NetherScales.netherChunkWidth(preset.chunkWidth(), preset.netherScale())),
                climateCompression);
        assertDimension(presetId, dimensions, "minecraft:the_end", "minecraft:end",
                boundsOfWidth.apply(preset.endChunkWidth()), climateCompression);
    }

    private static void assertDimension(String presetId, JsonObject dimensions, String dimensionId,
            String noiseSettingsId, WorldLoopBounds expected, @Nullable Boolean climateCompression) {
        String context = presetId + " " + dimensionId;
        JsonObject dimension = dimensions.getAsJsonObject(dimensionId);
        assertNotNull(dimension, context + ": dimension missing");
        assertEquals(dimensionId, dimension.get("type").getAsString(), context + ": dimension type");

        JsonObject generator = dimension.getAsJsonObject("generator");
        assertEquals(LOOPED_GENERATOR_ID, generator.get("type").getAsString(), context + ": generator type");
        assertEquals(noiseSettingsId, generator.get("settings").getAsString(), context + ": noise settings");

        WorldLoopBounds wrapping = WorldLoopBounds.CODEC.parse(JsonOps.INSTANCE, generator.get("wrapping"))
                .getOrThrow(message -> new AssertionError(context + ": wrapping does not parse: " + message));
        assertEquals(expected, wrapping, context + ": wrapping");

        if (climateCompression == null) {
            assertFalse(generator.has(CLIMATE_COMPRESSION_KEY), context + ": a cylinder states no climate choice");
            assertFalse(generator.has(GUARANTEED_LAND_KEY), context + ": a cylinder states no land choice");
            return;
        }

        assertNotNull(generator.get(CLIMATE_COMPRESSION_KEY), context + ": the climate choice is not stated");
        assertEquals(climateCompression, generator.get(CLIMATE_COMPRESSION_KEY).getAsBoolean(),
                context + ": climate compression");

        assertNotNull(generator.get(GUARANTEED_LAND_KEY), context + ": the land choice is not stated");
        assertEquals(TORUS_GUARANTEED_LAND, generator.get(GUARANTEED_LAND_KEY).getAsBoolean(),
                context + ": guaranteed land");
    }

    private static JsonObject readPresetJson(String presetId) throws IOException {
        String resource = PRESET_RESOURCE_DIR + presetId + ".json";
        try (InputStream stream = WorldLoopPresetsJsonTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "missing jar resource " + resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
