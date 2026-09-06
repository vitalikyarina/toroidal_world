package com.toroidalworld.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.noise.ClimateCompressionCacheFixture.Storing;
import com.toroidalworld.options.ClimateScale;
import com.toroidalworld.options.GenerationOptions;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;

class ClimateScaleCompressionTest {
    private static final double XZ_SCALE = 0.25;

    private static final double HORIZONTAL = 0.0;

    private static final double TOLERANCE = 1.0E-6;

    private static final boolean CLIMATE = true;
    private static final boolean COAST = false;

    private static final int[] PRESET_CHUNK_WIDTHS = {32, 64, 128, 256, 512};

    private record Octave(double amplitude, double cellBlocks) {
    }

    private record Field(String name, int firstOctave, DoubleList amplitudes, List<Octave> octaves,
            boolean climate) {
    }

    private static final Field TEMPERATURE = new Field("temperature", -10,
            DoubleArrayList.of(1.5, 0.0, 1.0, 0.0, 0.0, 0.0),
            List.of(new Octave(1.5, 4096.0), new Octave(1.0, 1024.0)), CLIMATE);

    private static final Field TEMPERATURE_LARGE = new Field("temperature_large", -12,
            DoubleArrayList.of(1.5, 0.0, 1.0, 0.0, 0.0, 0.0),
            List.of(new Octave(1.5, 16384.0), new Octave(1.0, 4096.0)), CLIMATE);

    private static final Field VEGETATION = new Field("vegetation", -8,
            DoubleArrayList.of(1.0, 1.0, 0.0, 0.0, 0.0, 0.0),
            List.of(new Octave(1.0, 1024.0), new Octave(1.0, 512.0)), CLIMATE);

    private static final Field CONTINENTALNESS = new Field("continentalness", -9,
            DoubleArrayList.of(1.0, 1.0, 2.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0),
            List.of(new Octave(1.0, 2048.0), new Octave(1.0, 1024.0), new Octave(2.0, 512.0),
                    new Octave(2.0, 256.0), new Octave(2.0, 128.0), new Octave(1.0, 64.0),
                    new Octave(1.0, 32.0), new Octave(1.0, 16.0), new Octave(1.0, 8.0)), COAST);

    private static final Field EROSION = new Field("erosion", -9,
            DoubleArrayList.of(1.0, 1.0, 0.0, 1.0, 1.0),
            List.of(new Octave(1.0, 2048.0), new Octave(1.0, 1024.0), new Octave(1.0, 256.0),
                    new Octave(1.0, 128.0)), COAST);

    private static final Field TEMPERATURE_NETHER = new Field("temperature_nether", -7,
            DoubleArrayList.of(1.0, 1.0),
            List.of(new Octave(1.0, 512.0), new Octave(1.0, 256.0)), CLIMATE);

    private static double expected(Field field, double lapBlocks) {
        double weighted = 0.0;
        double weight = 0.0;

        for (Octave octave : field.octaves()) {
            double square = octave.amplitude() * octave.amplitude();
            weighted += square * (lapBlocks / octave.cellBlocks());
            weight += square;
        }

        double cellsPerLap = weighted / weight;
        return Math.max(1.0, ClimateScaleCompression.CELLS_PER_LAP / cellsPerLap);
    }

    private static double actual(Field field, WorldFold fold, double verticalShare) {
        return ClimateScaleCompression.factor(fold, field.climate(), field.amplitudes(),
                Math.pow(2.0, field.firstOctave()), XZ_SCALE, verticalShare);
    }

    private static double resolved(Storing cache, Field field, WorldFold fold, double xzScale,
            double verticalShare) {
        return ClimateScaleCompression.resolve(cache, fold, field.climate(), field.amplitudes(),
                Math.pow(2.0, field.firstOctave()), xzScale, verticalShare);
    }

    private static WorldFold square(int chunkWidth) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunkWidth)));
    }

    private static WorldFold uncompressedSquare(int chunkWidth) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunkWidth)),
                GenerationOptions.DEFAULT.withClimateScale(ClimateScale.OFF));
    }

    private static WorldFold strongSquare(int chunkWidth) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunkWidth)),
                GenerationOptions.DEFAULT.withClimateScale(ClimateScale.STRONG));
    }

    private static WorldFold customSquare(int chunkWidth, int factor) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunkWidth)),
                GenerationOptions.DEFAULT.withClimateScale(ClimateScale.custom(factor)));
    }

    private static void assertFactor(Field field, int chunkWidth) {
        double lapBlocks = chunkWidth * 16.0;
        assertEquals(expected(field, lapBlocks), actual(field, square(chunkWidth), HORIZONTAL), TOLERANCE,
                field.name() + " on " + (int) lapBlocks + " blocks");
    }

    @Test
    void everyClimateFieldMatchesTheCellSizeArithmeticOnEveryPreset() {
        for (Field field : List.of(TEMPERATURE, TEMPERATURE_LARGE, VEGETATION, CONTINENTALNESS, EROSION)) {
            for (int chunkWidth : new int[] {32, 64, 128, 256, 512}) {
                assertFactor(field, chunkWidth);
            }
        }
    }

    @Test
    void aTorusThatDeclinedCompressionIsMultipliedByExactlyOneOnEveryFieldAndPreset() {
        for (Field field : List.of(TEMPERATURE, TEMPERATURE_LARGE, VEGETATION, CONTINENTALNESS, EROSION,
                TEMPERATURE_NETHER)) {
            for (int chunkWidth : new int[] {16, 32, 64, 128, 256, 512}) {
                assertEquals(1.0, actual(field, uncompressedSquare(chunkWidth), HORIZONTAL), 0.0,
                        field.name() + " uncompressed on " + chunkWidth * 16 + " blocks");
            }
        }
    }

    @Test
    void theFieldsThatShapeTheCoastAreLeftAloneOnTheNarrowestWorld() {
        assertEquals(1.0, actual(CONTINENTALNESS, square(32), HORIZONTAL), TOLERANCE, "continentalness tiny");
        assertEquals(1.0, actual(EROSION, square(32), HORIZONTAL), TOLERANCE, "erosion tiny");
    }

    @Test
    void temperatureIsUntouchedOnTheWidestPreset() {
        assertEquals(1.0, actual(TEMPERATURE, square(512), HORIZONTAL), TOLERANCE, "temperature huge");
        assertEquals(1.0, actual(TEMPERATURE, square(1024), HORIZONTAL), TOLERANCE, "16384 blocks by hand");
    }

    @Test
    void aFieldAtOrAboveTheTargetIsMultipliedByExactlyOne() {
        assertEquals(1.0, actual(TEMPERATURE, square(512), HORIZONTAL), 0.0, "temperature huge");
        assertEquals(1.0, actual(TEMPERATURE, square(1024), HORIZONTAL), 0.0, "16384 blocks by hand");
        assertEquals(1.0, actual(CONTINENTALNESS, square(32), HORIZONTAL), 0.0, "continentalness tiny");
        assertEquals(1.0, actual(EROSION, square(32), HORIZONTAL), 0.0, "erosion tiny");
    }

    @Test
    void theNetherIsMeasuredOnItsOwnNarrowerWidth() {
        assertEquals(expected(TEMPERATURE_NETHER, 256.0), actual(TEMPERATURE_NETHER, square(16), HORIZONTAL),
                TOLERANCE, "256-block nether under the three smallest presets");
        assertEquals(1.0, actual(TEMPERATURE_NETHER, square(64), HORIZONTAL), 0.0, "1024-block nether under huge");
    }

    @Test
    void aVerticallyLiveFieldIsNeverCompressed() {
        assertEquals(1.0, actual(TEMPERATURE, square(32), 0.5), TOLERANCE, "declared vertical share");
        assertEquals(1.0, actual(TEMPERATURE, square(32), GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE),
                TOLERANCE, "undeclared vertical share");
    }

    @Test
    void theShorterLapDrivesARectangularWorld() {
        WorldFold rectangular = WorldFolds.of(
                FlatShape.torus(new WorldLoopBounds(-16, 16, -8, 8)));

        assertEquals(expected(TEMPERATURE, 256.0), actual(TEMPERATURE, rectangular, HORIZONTAL), TOLERANCE,
                "256 blocks on Z against 512 on X");
    }

    @Test
    void theMemoAnswersWhatAFreshCallAnswersAndStoresOncePerBinding() {
        for (Field field : List.of(TEMPERATURE, TEMPERATURE_LARGE, VEGETATION, CONTINENTALNESS, EROSION,
                TEMPERATURE_NETHER)) {
            for (int chunkWidth : new int[] {16, 32, 64, 128, 256, 512}) {
                for (double share : new double[] {HORIZONTAL, 0.5,
                        GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE}) {
                    WorldFold fold = square(chunkWidth);
                    Storing cache = new Storing();
                    String where = field.name() + " on " + chunkWidth * 16 + " blocks, share " + share;

                    double fresh = actual(field, fold, share);
                    assertEquals(fresh, resolved(cache, field, fold, XZ_SCALE, share), 0.0, where + ", first call");
                    assertEquals(fresh, resolved(cache, field, fold, XZ_SCALE, share), 0.0, where + ", memo hit");
                    assertEquals(1, cache.stores, where + ", stores");
                }
            }
        }
    }

    @Test
    void aChangedFoldScaleOrShareIsResolvedAfresh() {
        Storing cache = new Storing();
        WorldFold small = square(32);
        WorldFold large = square(128);
        resolved(cache, TEMPERATURE, small, XZ_SCALE, HORIZONTAL);

        assertEquals(actual(TEMPERATURE, large, HORIZONTAL), resolved(cache, TEMPERATURE, large, XZ_SCALE, HORIZONTAL),
                0.0, "changed fold");
        assertEquals(2, cache.stores, "stores after the fold changed");

        assertEquals(1.0, resolved(cache, TEMPERATURE, large, XZ_SCALE, 0.5), 0.0, "changed share");
        assertEquals(3, cache.stores, "stores after the share changed");

        double halfScale = XZ_SCALE / 2.0;
        assertEquals(ClimateScaleCompression.factor(large, TEMPERATURE.climate(), TEMPERATURE.amplitudes(),
                Math.pow(2.0, TEMPERATURE.firstOctave()), halfScale, 0.5),
                resolved(cache, TEMPERATURE, large, halfScale, 0.5), 0.0, "changed scale");
        assertEquals(4, cache.stores, "stores after the scale changed");
    }

    @Test
    void aCylinderIsNeverCompressed() {
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            for (int chunkWidth : PRESET_CHUNK_WIDTHS) {
                WorldFold cylinder = WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(axis, chunkWidth)));
                String where = axis + " cylinder of " + chunkWidth * 16 + " blocks";

                assertEquals(1.0, actual(TEMPERATURE, cylinder, HORIZONTAL), 0.0, "temperature on a " + where);
                assertEquals(1.0, actual(VEGETATION, cylinder, HORIZONTAL), 0.0, "vegetation on a " + where);
            }
        }
    }

    @Test
    void strongIsAutoOrFourTimes_whicheverIsLarger_onEveryClimateFieldAndPreset() {
        for (Field field : List.of(TEMPERATURE, TEMPERATURE_LARGE, VEGETATION)) {
            for (int chunkWidth : PRESET_CHUNK_WIDTHS) {
                double auto = actual(field, square(chunkWidth), HORIZONTAL);
                double strong = actual(field, strongSquare(chunkWidth), HORIZONTAL);
                String where = field.name() + " on " + chunkWidth * 16 + " blocks";

                assertEquals(Math.max(auto, ClimateScale.STRONG_FACTOR), strong, TOLERANCE, where);
                assertTrue(strong >= auto, where + ": strong is weaker than auto");
            }
        }
    }

    @Test
    void strongShrinksTheWidestPresetsWhereAutoDoesNothing() {
        for (int chunkWidth : new int[] {256, 512}) {
            String where = "temperature on " + chunkWidth * 16 + " blocks";

            assertEquals(1.0, actual(TEMPERATURE, square(chunkWidth), HORIZONTAL), 0.0, where + ", auto");
            assertEquals(ClimateScale.STRONG_FACTOR, actual(TEMPERATURE, strongSquare(chunkWidth), HORIZONTAL),
                    0.0, where + ", strong");
        }
    }

    @Test
    void strongLeavesTheFieldsThatShapeTheCoastAloneOnEveryPreset() {
        for (Field field : List.of(CONTINENTALNESS, EROSION)) {
            for (int chunkWidth : PRESET_CHUNK_WIDTHS) {
                assertEquals(1.0, actual(field, strongSquare(chunkWidth), HORIZONTAL), 0.0,
                        field.name() + " under strong on " + chunkWidth * 16 + " blocks");
            }
        }
    }

    @Test
    void customAppliesTheTypedFactorExactlyAndIsNeverCombinedWithAuto() {
        for (int chunkWidth : PRESET_CHUNK_WIDTHS) {
            assertEquals(6.0, actual(TEMPERATURE, customSquare(chunkWidth, 6), HORIZONTAL), 0.0,
                    "temperature under custom 6 on " + chunkWidth * 16 + " blocks");
        }

        assertEquals(2.0, actual(TEMPERATURE, customSquare(32, 2), HORIZONTAL), 0.0,
                "custom 2 on a tiny world, where auto asks for more");
    }

    @Test
    void customLeavesTheFieldsThatShapeTheCoastAloneOnEveryPreset() {
        for (Field field : List.of(CONTINENTALNESS, EROSION)) {
            for (int chunkWidth : PRESET_CHUNK_WIDTHS) {
                assertEquals(1.0, actual(field, customSquare(chunkWidth, 16), HORIZONTAL), 0.0,
                        field.name() + " under custom 16 on " + chunkWidth * 16 + " blocks");
            }
        }
    }

    @Test
    void aFixedModeIsRefusedByAVerticallyLiveFieldAndByACylinder() {
        assertEquals(1.0, actual(TEMPERATURE, strongSquare(32), 0.5), 0.0, "strong, declared vertical share");
        assertEquals(1.0, actual(TEMPERATURE, customSquare(32, 8), 0.5), 0.0, "custom, declared vertical share");

        WorldFold cylinder = WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 32)),
                GenerationOptions.DEFAULT.withClimateScale(ClimateScale.STRONG));
        assertEquals(1.0, actual(TEMPERATURE, cylinder, HORIZONTAL), 0.0, "strong on a cylinder");
    }

    @Test
    void offIsExactlyOneInEveryModeAwareBranch() {
        for (Field field : List.of(TEMPERATURE, VEGETATION, CONTINENTALNESS, EROSION)) {
            for (int chunkWidth : PRESET_CHUNK_WIDTHS) {
                assertEquals(1.0, actual(field, uncompressedSquare(chunkWidth), HORIZONTAL), 0.0,
                        field.name() + " off on " + chunkWidth * 16 + " blocks");
            }
        }
    }
}
