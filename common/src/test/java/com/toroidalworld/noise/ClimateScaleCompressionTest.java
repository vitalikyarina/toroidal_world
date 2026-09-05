package com.toroidalworld.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;

class ClimateScaleCompressionTest {
    private static final double XZ_SCALE = 0.25;

    private static final double HORIZONTAL = 0.0;

    private static final double TOLERANCE = 1.0E-6;

    private record Octave(double amplitude, double cellBlocks) {
    }

    private record Field(String name, int firstOctave, DoubleList amplitudes, List<Octave> octaves) {
    }

    private static final Field TEMPERATURE = new Field("temperature", -10,
            DoubleArrayList.of(1.5, 0.0, 1.0, 0.0, 0.0, 0.0),
            List.of(new Octave(1.5, 4096.0), new Octave(1.0, 1024.0)));

    private static final Field TEMPERATURE_LARGE = new Field("temperature_large", -12,
            DoubleArrayList.of(1.5, 0.0, 1.0, 0.0, 0.0, 0.0),
            List.of(new Octave(1.5, 16384.0), new Octave(1.0, 4096.0)));

    private static final Field VEGETATION = new Field("vegetation", -8,
            DoubleArrayList.of(1.0, 1.0, 0.0, 0.0, 0.0, 0.0),
            List.of(new Octave(1.0, 1024.0), new Octave(1.0, 512.0)));

    private static final Field CONTINENTALNESS = new Field("continentalness", -9,
            DoubleArrayList.of(1.0, 1.0, 2.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0),
            List.of(new Octave(1.0, 2048.0), new Octave(1.0, 1024.0), new Octave(2.0, 512.0),
                    new Octave(2.0, 256.0), new Octave(2.0, 128.0), new Octave(1.0, 64.0),
                    new Octave(1.0, 32.0), new Octave(1.0, 16.0), new Octave(1.0, 8.0)));

    private static final Field EROSION = new Field("erosion", -9,
            DoubleArrayList.of(1.0, 1.0, 0.0, 1.0, 1.0),
            List.of(new Octave(1.0, 2048.0), new Octave(1.0, 1024.0), new Octave(1.0, 256.0),
                    new Octave(1.0, 128.0)));

    private static final Field TEMPERATURE_NETHER = new Field("temperature_nether", -7,
            DoubleArrayList.of(1.0, 1.0),
            List.of(new Octave(1.0, 512.0), new Octave(1.0, 256.0)));

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
        return ClimateScaleCompression.factor(fold.blockDomain(Direction.Axis.X),
                fold.blockDomain(Direction.Axis.Z), field.amplitudes(),
                Math.pow(2.0, field.firstOctave()), XZ_SCALE, verticalShare);
    }

    private static WorldFold square(int chunkWidth) {
        return WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(chunkWidth)));
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
    void aCylinderIsNeverCompressed() {
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            for (int chunkWidth : new int[] {32, 64, 128, 256, 512}) {
                WorldFold cylinder = WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(axis, chunkWidth)));
                String where = axis + " cylinder of " + chunkWidth * 16 + " blocks";

                assertEquals(1.0, actual(TEMPERATURE, cylinder, HORIZONTAL), 0.0, "temperature on a " + where);
                assertEquals(1.0, actual(VEGETATION, cylinder, HORIZONTAL), 0.0, "vegetation on a " + where);
            }
        }
    }
}
