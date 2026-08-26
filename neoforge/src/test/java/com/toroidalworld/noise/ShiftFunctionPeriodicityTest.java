package com.toroidalworld.noise;

import static com.toroidalworld.noise.DensityFunctionFixture.NOISE_DATA;
import static com.toroidalworld.noise.DensityFunctionFixture.SEED;
import static com.toroidalworld.noise.DensityFunctionFixture.SQUARE;
import static com.toroidalworld.noise.DensityFunctionFixture.WORLDS;
import static com.toroidalworld.noise.DensityFunctionFixture.blockIn;
import static com.toroidalworld.noise.DensityFunctionFixture.blockY;
import static com.toroidalworld.noise.DensityFunctionFixture.withLiveNoise;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

class ShiftFunctionPeriodicityTest {
    private static final int SAMPLES = 64;
    private static final double MIN_SPREAD = 0.1;

    private record ShiftFunction(String name, DensityFunction function) {
    }

    private static final List<ShiftFunction> SHIFTS = List.of(
            new ShiftFunction("shift", withLiveNoise(DensityFunctions.shift(NOISE_DATA))),
            new ShiftFunction("shift_a", withLiveNoise(DensityFunctions.shiftA(NOISE_DATA))),
            new ShiftFunction("shift_b", withLiveNoise(DensityFunctions.shiftB(NOISE_DATA))));

    @Test
    void everyShiftFunctionAgreesOneWorldWidthApartInX() {
        Random random = new Random(SEED);
        for (WorldFold transformer : WORLDS) {
            int width = transformer.blockDomain(Direction.Axis.X).domainLength;
            for (ShiftFunction shift : SHIFTS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = blockIn(random, transformer.blockDomain(Direction.Axis.X));
                    int y = blockY(random);
                    int z = blockIn(random, transformer.blockDomain(Direction.Axis.Z));
                    assertEquals(sample(shift, transformer, x, y, z),
                            sample(shift, transformer, x + width, y, z),
                            at(shift, transformer, "x", x, x + width, y, z));
                }
            }
        }
    }

    @Test
    void everyShiftFunctionAgreesOneWorldWidthApartInZ() {
        Random random = new Random(SEED);
        for (WorldFold transformer : WORLDS) {
            int width = transformer.blockDomain(Direction.Axis.Z).domainLength;
            for (ShiftFunction shift : SHIFTS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = blockIn(random, transformer.blockDomain(Direction.Axis.X));
                    int y = blockY(random);
                    int z = blockIn(random, transformer.blockDomain(Direction.Axis.Z));
                    assertEquals(sample(shift, transformer, x, y, z),
                            sample(shift, transformer, x, y, z + width),
                            at(shift, transformer, "z", z, z + width, x, y));
                }
            }
        }
    }

    @Test
    void everyShiftFunctionSamplesAFieldThatVaries() {
        Random random = new Random(SEED);
        for (ShiftFunction shift : SHIFTS) {
            double lowest = Double.MAX_VALUE;
            double highest = -Double.MAX_VALUE;
            for (int i = 0; i < SAMPLES; i++) {
                double value = sample(shift, SQUARE,
                        blockIn(random, SQUARE.blockDomain(Direction.Axis.X)), blockY(random),
                        blockIn(random, SQUARE.blockDomain(Direction.Axis.Z)));
                lowest = Math.min(lowest, value);
                highest = Math.max(highest, value);
            }

            assertTrue(highest - lowest > MIN_SPREAD,
                    shift.name() + " sampled a near-constant field, spread " + (highest - lowest));
        }
    }

    private static double sample(ShiftFunction shift, WorldFold transformer, int x, int y, int z) {
        DensityFunction.FunctionContext at = new DensityFunction.SinglePointContext(x, y, z);

        return GenerationTransformerContext.withTransformer(transformer, () -> shift.function().compute(at));
    }

    private static String at(ShiftFunction shift, WorldFold transformer,
            String axis, int from, int to, int firstOther, int secondOther) {
        return shift.name() + " in " + transformer + " at " + axis + "=" + from + " vs " + axis + "=" + to
                + " (" + firstOther + ", " + secondOther + ")";
    }
}
