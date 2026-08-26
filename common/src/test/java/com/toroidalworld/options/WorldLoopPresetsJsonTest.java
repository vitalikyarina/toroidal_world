package com.toroidalworld.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.function.IntFunction;

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

    @Test
    void everyPresetShipsATorusWorldPresetMatchingItsConfiguration() throws IOException {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            assertPreset(preset.id(), preset, WorldLoopBounds::ofWidth);
        }
    }

    @Test
    void everyPresetShipsACylinderWorldPresetOnTheDefaultAxis() throws IOException {
        Direction.Axis axis = CylinderSettings.DEFAULT.axis();
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            assertPreset(CYLINDER_PRESET_PREFIX + preset.id(), preset, width -> WorldLoopBounds.ofWidth(axis, width));
        }
    }

    private static void assertPreset(String presetId, WorldLoopPresets preset, IntFunction<WorldLoopBounds> boundsOfWidth)
            throws IOException {
        JsonObject dimensions = readPresetJson(presetId).getAsJsonObject("dimensions");
        assertNotNull(dimensions, presetId + ": no dimensions object");
        assertEquals(3, dimensions.size(), presetId + ": expected exactly the three vanilla dimensions");

        assertDimension(presetId, dimensions, "minecraft:overworld", "minecraft:overworld",
                boundsOfWidth.apply(preset.chunkWidth()));
        assertDimension(presetId, dimensions, "minecraft:the_nether", "minecraft:nether",
                boundsOfWidth.apply(NetherScales.netherChunkWidth(preset.chunkWidth(), preset.netherScale())));
        assertDimension(presetId, dimensions, "minecraft:the_end", "minecraft:end",
                boundsOfWidth.apply(preset.endChunkWidth()));
    }

    private static void assertDimension(String presetId, JsonObject dimensions, String dimensionId,
            String noiseSettingsId, WorldLoopBounds expected) {
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
    }

    private static JsonObject readPresetJson(String presetId) throws IOException {
        String resource = PRESET_RESOURCE_DIR + presetId + ".json";
        try (InputStream stream = WorldLoopPresetsJsonTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "missing jar resource " + resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
