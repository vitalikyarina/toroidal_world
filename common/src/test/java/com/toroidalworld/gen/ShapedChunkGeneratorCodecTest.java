package com.toroidalworld.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.GenerationOptions;
import com.toroidalworld.options.ClimateScale;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopSizes;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;
import com.toroidalworld.shape.FlatShape.Identification;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;

class ShapedChunkGeneratorCodecTest {
    private static final WorldLoopBounds SQUARE = WorldLoopBounds.ofWidth(32);
    private static final WorldLoopBounds X_ONLY =
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE);

    private static final HolderLookup.Provider WORLDGEN = VanillaRegistries.createLookup();

    private static final String FROZEN_ON_DISK_KEY = "wrapping";
    private static final String CLIMATE_SCALE_KEY = "climate_compression";
    private static final String GUARANTEED_LAND_KEY = "guaranteed_land";
    private static final String MODE_KEY = "mode";
    private static final String FACTOR_KEY = "factor";

    private static final int CUSTOM_FACTOR = 6;

    private static final String TORUS_WRAPPING =
            "{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},\"z\":{\"min_chunk\":-16,\"max_chunk\":16}}";
    private static final String TOO_NARROW_WRAPPING =
            "{\"x\":{\"min_chunk\":-4,\"max_chunk\":4},\"z\":{\"min_chunk\":-4,\"max_chunk\":4}}";
    private static final String SKEWED_WRAPPING =
            "{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},\"z\":{\"min_chunk\":-16,\"max_chunk\":16},"
                    + "\"skew_chunks\":5}";

    @Test
    void theShapeFieldCarriesEveryShapeTheEngineCanFold() {
        for (FlatShape shape : List.of(FlatShape.torus(SQUARE), FlatShape.cylinder(X_ONLY))) {
            JsonElement written =
                    ShapedChunkGenerator.SHAPE_CODEC.encodeStart(JsonOps.INSTANCE, shape).getOrThrow();
            assertEquals(shape, ShapedChunkGenerator.SHAPE_CODEC.parse(JsonOps.INSTANCE, written).getOrThrow(),
                    written.toString());
        }
    }

    @Test
    void aTorusFieldIsStillTheLegacyWrappingValue() {
        assertEquals(WorldLoopBounds.CODEC.encodeStart(JsonOps.INSTANCE, SQUARE).getOrThrow(),
                ShapedChunkGenerator.SHAPE_CODEC.encodeStart(JsonOps.INSTANCE, FlatShape.torus(SQUARE))
                        .getOrThrow());
    }

    @Test
    void aWorldFileCarryingASkewedLatticeRefusesToLoad() {
        assertTrue(readError("{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},"
                + "\"z\":{\"min_chunk\":-16,\"max_chunk\":16},\"skew_chunks\":5}")
                .contains(FlatShape.Identification.LATTICE_TORUS.toString()));
    }

    @Test
    void aWorldFileCarryingAMirrorRefusesToLoad() {
        assertTrue(readError("{\"x\":{},\"z\":{\"min_chunk\":-16,\"max_chunk\":16},"
                + "\"mirror\":{\"axis\":\"x\",\"line_chunk\":3}}")
                .contains(FlatShape.Identification.MOBIUS.toString()));
    }

    @Test
    void aMirroredWorldFileIsRefusedOnTheLocalIndexFloorRatherThanOnDecomposition() {
        FlatShape klein = FlatShape.mirrored(SQUARE, Direction.Axis.Z, -7);

        assertEquals(
                WorldFolds.verifyPreservesLocalIndices(klein).error().orElseThrow().message(),
                readError("{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},\"z\":{\"min_chunk\":-16,\"max_chunk\":16},"
                        + "\"mirror\":{\"axis\":\"z\",\"line_chunk\":-7}}"));
    }

    @Test
    void aCoupledShapeCannotEvenBeWrittenIntoTheField() {
        DataResult<JsonElement> written = ShapedChunkGenerator.SHAPE_CODEC.encodeStart(JsonOps.INSTANCE,
                FlatShape.mirrored(SQUARE, Direction.Axis.Z, -7));

        assertTrue(written.isError(), written.toString());
        assertTrue(written.error().orElseThrow().message()
                .contains(FlatShape.Identification.KLEIN.toString()), written.toString());
    }

    @Test
    void theNoiseGeneratorReadsItsShapeThroughTheGate() {
        assertGated(LoopedChunkGenerator.CODEC);
    }

    @Test
    void theFlatGeneratorReadsItsShapeThroughTheGate() {
        assertGated(LoopedFlatChunkGenerator.CODEC);
    }

    private static void assertGated(MapCodec<?> generatorCodec) {
        String coupled = generatorError(generatorCodec, SKEWED_WRAPPING);
        assertTrue(coupled.contains(Identification.LATTICE_TORUS.toString()), coupled);

        String narrow = generatorError(generatorCodec, TOO_NARROW_WRAPPING);
        assertTrue(narrow.contains(WorldLoopSizes.describe(WorldLoopSizes.MIN_CHUNK_WIDTH)), narrow);

        String torus = generatorError(generatorCodec, TORUS_WRAPPING);
        assertFalse(torus.contains(Identification.LATTICE_TORUS.toString()), torus);
    }

    private static String generatorError(MapCodec<?> generatorCodec, String wrapping) {
        DataResult<?> result = generatorCodec.codec()
                .parse(JsonOps.INSTANCE, JsonParser.parseString("{\"wrapping\":" + wrapping + "}"));

        assertTrue(result.isError(), result.toString());
        return result.error().orElseThrow().message();
    }

    @Test
    void aNoiseGeneratorCannotBeBuiltAroundACoupledShape() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> noiseGenerator(FlatShape.latticeTorus(SQUARE, 5)));

        assertTrue(refused.getMessage().contains(Identification.LATTICE_TORUS.toString()), refused.getMessage());
    }

    @Test
    void aFlatGeneratorCannotBeBuiltAroundACoupledShape() {
        IllegalArgumentException refused = assertThrows(IllegalArgumentException.class,
                () -> flatGenerator(FlatShape.mirrored(SQUARE, Direction.Axis.Z, -7)));

        assertTrue(refused.getMessage().contains(Identification.KLEIN.toString()), refused.getMessage());
    }

    @Test
    void aTorusSitsUnderTheSameKeyAndValueInTheGeneratorsOwnEncoding() {
        RegistryOps<JsonElement> ops = WORLDGEN.createSerializationContext(JsonOps.INSTANCE);
        FlatShape torus = FlatShape.torus(SQUARE);

        JsonElement encoded =
                LoopedChunkGenerator.CODEC.codec().encodeStart(ops, noiseGenerator(torus)).getOrThrow();

        assertEquals(WorldLoopBounds.CODEC.encodeStart(JsonOps.INSTANCE, SQUARE).getOrThrow(),
                encoded.getAsJsonObject().get(FROZEN_ON_DISK_KEY),
                encoded.toString());
        assertEquals(torus, LoopedChunkGenerator.CODEC.codec().parse(ops, encoded).getOrThrow().shape());
    }

    @Test
    void aSuperflatTorusSitsThereToo() {
        RegistryOps<JsonElement> ops = WORLDGEN.createSerializationContext(JsonOps.INSTANCE);
        FlatShape torus = FlatShape.torus(SQUARE);

        JsonElement encoded =
                LoopedFlatChunkGenerator.CODEC.codec().encodeStart(ops, flatGenerator(torus)).getOrThrow();

        assertEquals(WorldLoopBounds.CODEC.encodeStart(JsonOps.INSTANCE, SQUARE).getOrThrow(),
                encoded.getAsJsonObject().get(FROZEN_ON_DISK_KEY),
                encoded.toString());
        assertEquals(torus, LoopedFlatChunkGenerator.CODEC.codec().parse(ops, encoded).getOrThrow().shape());
    }

    @Test
    void aTorusThatCompressesItsClimateWritesTheLayoutOfASaveMadeBeforeTheChoiceExisted() {
        RegistryOps<JsonElement> ops = WORLDGEN.createSerializationContext(JsonOps.INSTANCE);

        JsonElement encoded = LoopedChunkGenerator.CODEC.codec()
                .encodeStart(ops, noiseGenerator(FlatShape.torus(SQUARE), GenerationOptions.DEFAULT.withClimateScale(ClimateScale.AUTO))).getOrThrow();

        assertFalse(encoded.getAsJsonObject().has(CLIMATE_SCALE_KEY), encoded.toString());
        assertEquals(ClimateScale.AUTO,
                LoopedChunkGenerator.CODEC.codec().parse(ops, encoded).getOrThrow().generationOptions().climateScale());
    }

    @Test
    void aTorusThatDeclinedCompressionWritesTheChoiceAndReadsItBack() {
        RegistryOps<JsonElement> ops = WORLDGEN.createSerializationContext(JsonOps.INSTANCE);

        JsonElement encoded = LoopedChunkGenerator.CODEC.codec()
                .encodeStart(ops, noiseGenerator(FlatShape.torus(SQUARE), GenerationOptions.DEFAULT.withClimateScale(ClimateScale.OFF))).getOrThrow();

        assertFalse(encoded.getAsJsonObject().get(CLIMATE_SCALE_KEY).getAsBoolean(), encoded.toString());
        assertEquals(ClimateScale.OFF,
                LoopedChunkGenerator.CODEC.codec().parse(ops, encoded).getOrThrow().generationOptions().climateScale());
    }

    @Test
    void aWorldFileWithoutTheChoiceLoadsCompressed() {
        RegistryOps<JsonElement> ops = WORLDGEN.createSerializationContext(JsonOps.INSTANCE);
        JsonElement encoded = LoopedChunkGenerator.CODEC.codec()
                .encodeStart(ops, noiseGenerator(FlatShape.torus(SQUARE), GenerationOptions.DEFAULT.withClimateScale(ClimateScale.OFF))).getOrThrow();
        encoded.getAsJsonObject().remove(CLIMATE_SCALE_KEY);

        assertEquals(ClimateScale.AUTO,
                LoopedChunkGenerator.CODEC.codec().parse(ops, encoded).getOrThrow().generationOptions().climateScale());
    }

    @Test
    void aTorusThatGuaranteesLandWritesTheChoiceAndReadsItBack() {
        RegistryOps<JsonElement> ops = WORLDGEN.createSerializationContext(JsonOps.INSTANCE);

        JsonElement encoded = LoopedChunkGenerator.CODEC.codec()
                .encodeStart(ops, noiseGenerator(FlatShape.torus(SQUARE),
                        GenerationOptions.DEFAULT.withGuaranteedLand(true))).getOrThrow();

        assertTrue(encoded.getAsJsonObject().get(GUARANTEED_LAND_KEY).getAsBoolean(), encoded.toString());
        assertTrue(LoopedChunkGenerator.CODEC.codec().parse(ops, encoded).getOrThrow()
                .generationOptions().guaranteedLand());
    }

    @Test
    void aWorldFileWithoutTheLandChoiceLoadsWithoutTheGuarantee() {
        RegistryOps<JsonElement> ops = WORLDGEN.createSerializationContext(JsonOps.INSTANCE);

        JsonElement encoded = LoopedChunkGenerator.CODEC.codec()
                .encodeStart(ops, noiseGenerator(FlatShape.torus(SQUARE), GenerationOptions.DEFAULT)).getOrThrow();

        assertFalse(encoded.getAsJsonObject().has(GUARANTEED_LAND_KEY), encoded.toString());
        assertFalse(LoopedChunkGenerator.CODEC.codec().parse(ops, encoded).getOrThrow()
                .generationOptions().guaranteedLand());
    }

    @Test
    void aSuperflatTorusCarriesTheChoiceTheSameWay() {
        RegistryOps<JsonElement> ops = WORLDGEN.createSerializationContext(JsonOps.INSTANCE);

        JsonElement encoded = LoopedFlatChunkGenerator.CODEC.codec()
                .encodeStart(ops, flatGenerator(FlatShape.torus(SQUARE), GenerationOptions.DEFAULT.withClimateScale(ClimateScale.OFF))).getOrThrow();

        assertFalse(encoded.getAsJsonObject().get(CLIMATE_SCALE_KEY).getAsBoolean(), encoded.toString());
        assertEquals(ClimateScale.OFF,
                LoopedFlatChunkGenerator.CODEC.codec().parse(ops, encoded).getOrThrow().generationOptions().climateScale());
    }

    private static LoopedChunkGenerator noiseGenerator(FlatShape shape) {
        return noiseGenerator(shape, GenerationOptions.DEFAULT);
    }

    private static LoopedChunkGenerator noiseGenerator(FlatShape shape, GenerationOptions generationOptions) {
        return new LoopedChunkGenerator(
                new FixedBiomeSource(WORLDGEN.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS)),
                WORLDGEN.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.OVERWORLD),
                shape, generationOptions);
    }

    private static LoopedFlatChunkGenerator flatGenerator(FlatShape shape) {
        return flatGenerator(shape, GenerationOptions.DEFAULT);
    }

    private static LoopedFlatChunkGenerator flatGenerator(FlatShape shape, GenerationOptions generationOptions) {
        return new LoopedFlatChunkGenerator(new FlatLevelGeneratorSettings(
                Optional.empty(),
                WORLDGEN.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS),
                List.of()), shape, generationOptions);
    }

    private static String readError(String json) {
        DataResult<FlatShape> result =
                ShapedChunkGenerator.SHAPE_CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
        assertTrue(result.isError(), result.toString());
        return result.error().orElseThrow().message();
    }
}
