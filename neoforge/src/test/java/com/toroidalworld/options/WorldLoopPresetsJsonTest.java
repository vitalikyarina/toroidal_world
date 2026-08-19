package com.toroidalworld.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

class WorldLoopPresetsJsonTest {
    private static final String PRESET_RESOURCE_DIR = "/data/toroidal_world/worldgen/world_preset/";
    private static final String LOOPED_GENERATOR_ID = "toroidal_world:toroidal";

    @Test
    void everyPresetShipsAJarWorldPresetMatchingItsConfiguration() throws IOException {
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            JsonObject dimensions = readPresetJson(preset.id()).getAsJsonObject("dimensions");
            assertNotNull(dimensions, preset.id() + ": no dimensions object");
            assertEquals(3, dimensions.size(), preset.id() + ": expected exactly the three vanilla dimensions");

            assertDimension(preset.id(), dimensions, "minecraft:overworld", "minecraft:overworld",
                    preset.chunkWidth());
            assertDimension(preset.id(), dimensions, "minecraft:the_nether", "minecraft:nether",
                    NetherScales.netherChunkWidth(preset.chunkWidth(), preset.netherScale()));
            assertDimension(preset.id(), dimensions, "minecraft:the_end", "minecraft:end",
                    preset.endChunkWidth());
        }
    }

    private static void assertDimension(String presetId, JsonObject dimensions, String dimensionId,
            String noiseSettingsId, int chunkWidth) {
        String context = presetId + " " + dimensionId;
        JsonObject dimension = dimensions.getAsJsonObject(dimensionId);
        assertNotNull(dimension, context + ": dimension missing");
        assertEquals(dimensionId, dimension.get("type").getAsString(), context + ": dimension type");

        JsonObject generator = dimension.getAsJsonObject("generator");
        assertEquals(LOOPED_GENERATOR_ID, generator.get("type").getAsString(), context + ": generator type");
        assertEquals(noiseSettingsId, generator.get("settings").getAsString(), context + ": noise settings");

        AxisBounds.Looped expected = (AxisBounds.Looped) WorldLoopBounds.ofWidth(chunkWidth).x();
        JsonObject wrapping = generator.getAsJsonObject("wrapping");
        for (String axis : new String[] {"x", "z"}) {
            JsonObject bounds = wrapping.getAsJsonObject(axis);
            assertEquals(expected.minChunk(), bounds.get("min_chunk").getAsInt(), context + ": " + axis + " min");
            assertEquals(expected.maxChunk(), bounds.get("max_chunk").getAsInt(), context + ": " + axis + " max");
        }
    }

    private static JsonObject readPresetJson(String presetId) throws IOException {
        String resource = PRESET_RESOURCE_DIR + presetId + ".json";
        try (InputStream stream = WorldLoopPresetsJsonTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "missing jar resource " + resource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
