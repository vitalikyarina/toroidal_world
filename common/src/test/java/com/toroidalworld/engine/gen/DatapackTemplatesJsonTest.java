package com.toroidalworld.engine.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.CarriedShape;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.WorldOption;
import com.toroidalworld.core.WorldOptions;

import net.minecraft.SharedConstants;
import net.minecraft.server.packs.PackType;

class DatapackTemplatesJsonTest {
    private static final String REPO_ROOT_PROPERTY = "toroidal.repoRoot";
    private static final String TEMPLATE_DIR = "docs/datapacks";
    private static final String PRESET_DIR = "data/my_pack/worldgen/world_preset";
    private static final String PACK_METADATA = "pack.mcmeta";

    private static final String TORUS = "torus";
    private static final String CYLINDER = "cylinder";

    private static final String GENERATOR_ID = "toroidal_world:toroidal";
    private static final int VANILLA_DIMENSIONS = 3;

    @Test
    void theTorusTemplateLoopsBothAxesAndStatesEveryWorldOption() throws IOException {
        assertTemplate(TORUS, FlatShape.Identification.TORUS, true);
    }

    @Test
    void theCylinderTemplateLoopsOneAxisAndStatesNoWorldOption() throws IOException {
        assertTemplate(CYLINDER, FlatShape.Identification.CYLINDER, false);
    }

    @Test
    void bothTemplatesDeclareTheDatapackFormatTheGameReads() throws IOException {
        int format = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA).major();

        for (String template : List.of(TORUS, CYLINDER)) {
            JsonObject pack = readJson(templateDir(template).resolve(PACK_METADATA)).getAsJsonObject("pack");
            assertNotNull(pack, template + ": no pack object");
            assertEquals(format, pack.get("min_format").getAsInt(), template + ": min_format");
            assertEquals(format, pack.get("max_format").getAsInt(), template + ": max_format");
        }
    }

    private static void assertTemplate(String template, FlatShape.Identification identification, boolean statesOptions)
            throws IOException {
        Path preset = templateDir(template).resolve(PRESET_DIR).resolve(template + ".json");
        JsonObject dimensions = readJson(preset).getAsJsonObject("dimensions");
        assertNotNull(dimensions, template + ": no dimensions object");
        assertEquals(VANILLA_DIMENSIONS, dimensions.size(),
                template + ": expected exactly the three vanilla dimensions");

        WorldLoopBounds overworld = assertDimension(template, dimensions, "minecraft:overworld", "minecraft:overworld",
                identification, statesOptions);
        WorldLoopBounds nether = assertDimension(template, dimensions, "minecraft:the_nether", "minecraft:nether",
                identification, statesOptions);
        WorldLoopBounds end = assertDimension(template, dimensions, "minecraft:the_end", "minecraft:end",
                identification, statesOptions);

        WorldShapeReport.Note scale = WorldShapeReport.netherScaleNote(overworld, nether);
        assertFalse(scale.broken(), template + scale.text());

        WorldShapeReport.Note width = WorldShapeReport.endWidthNote(end);
        assertFalse(width.broken(), template + width.text());
    }

    private static WorldLoopBounds assertDimension(String template, JsonObject dimensions, String dimensionId,
            String noiseSettingsId, FlatShape.Identification identification, boolean statesOptions) {
        String context = template + " " + dimensionId;
        JsonObject dimension = dimensions.getAsJsonObject(dimensionId);
        assertNotNull(dimension, context + ": dimension missing");
        assertEquals(dimensionId, dimension.get("type").getAsString(), context + ": dimension type");

        JsonObject generator = dimension.getAsJsonObject("generator");
        assertEquals(GENERATOR_ID, generator.get("type").getAsString(), context + ": generator type");
        assertEquals(noiseSettingsId, generator.get("settings").getAsString(), context + ": noise settings");

        FlatShape shape = CarriedShape.SHAPE_CODEC
                .parse(JsonOps.INSTANCE, generator.get(CarriedShape.WRAPPING_KEY))
                .getOrThrow(message -> new AssertionError(context + ": wrapping is refused: " + message));
        assertEquals(identification, shape.identification(), context + ": identification");

        for (WorldOption<?> option : WorldOptions.all()) {
            assertOption(context, generator, option, statesOptions);
        }

        return shape.bounds();
    }

    private static <V> void assertOption(String context, JsonObject generator, WorldOption<V> option, boolean stated) {
        JsonElement value = generator.get(option.key());
        if (!stated) {
            assertNull(value, context + ": a cylinder states no " + option.key());
            return;
        }

        assertNotNull(value, context + ": " + option.key() + " is not stated");
        option.codec().parse(JsonOps.INSTANCE, value).getOrThrow(
                message -> new AssertionError(context + ": " + option.key() + " does not parse: " + message));
    }

    private static Path templateDir(String template) {
        String root = System.getProperty(REPO_ROOT_PROPERTY);
        assertNotNull(root, "the " + REPO_ROOT_PROPERTY + " system property names no repository root");
        return Path.of(root, TEMPLATE_DIR, template);
    }

    private static JsonObject readJson(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "missing template file " + path);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
