package com.toroidalworld.engine.noise;

import com.toroidalworld.core.WorldFold;
import static com.toroidalworld.engine.noise.DensityFunctionFixture.CLIMATE_XZ_SCALE;
import static com.toroidalworld.engine.noise.DensityFunctionFixture.NOISE_DATA;
import static com.toroidalworld.engine.noise.DensityFunctionFixture.SEED;
import static com.toroidalworld.engine.noise.DensityFunctionFixture.SQUARE;
import static com.toroidalworld.engine.noise.DensityFunctionFixture.WORLDS;
import static com.toroidalworld.engine.noise.DensityFunctionFixture.blockIn;
import static com.toroidalworld.engine.noise.DensityFunctionFixture.blockY;
import static com.toroidalworld.engine.noise.DensityFunctionFixture.withLiveNoise;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;


import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

class ShiftedNoisePeriodicityTest {
    private static final int SAMPLES = 64;
    private static final double MIN_WARPED_SHARE = 0.9;

    private static final DensityFunction WARPED = withLiveNoise(DensityFunctions.shiftedNoise2d(
            DensityFunctions.shiftA(NOISE_DATA), DensityFunctions.shiftB(NOISE_DATA), CLIMATE_XZ_SCALE, NOISE_DATA));

    private static final DensityFunction UNWARPED = withLiveNoise(DensityFunctions.shiftedNoise2d(
            DensityFunctions.zero(), DensityFunctions.zero(), CLIMATE_XZ_SCALE, NOISE_DATA));

    @Test
    void warpedNoiseAgreesOneWorldWidthApartInX() {
        Random random = new Random(SEED);
        for (WorldFold transformer : WORLDS) {
            int width = transformer.blockDomain(Direction.Axis.X).domainLength;
            for (int i = 0; i < SAMPLES; i++) {
                int x = blockIn(random, transformer.blockDomain(Direction.Axis.X));
                int y = blockY(random);
                int z = blockIn(random, transformer.blockDomain(Direction.Axis.Z));
                assertEquals(sample(WARPED, transformer, x, y, z), sample(WARPED, transformer, x + width, y, z),
                        at(transformer, "x", x, x + width, y, z));
            }
        }
    }

    @Test
    void warpedNoiseAgreesOneWorldWidthApartInZ() {
        Random random = new Random(SEED);
        for (WorldFold transformer : WORLDS) {
            int width = transformer.blockDomain(Direction.Axis.Z).domainLength;
            for (int i = 0; i < SAMPLES; i++) {
                int x = blockIn(random, transformer.blockDomain(Direction.Axis.X));
                int y = blockY(random);
                int z = blockIn(random, transformer.blockDomain(Direction.Axis.Z));
                assertEquals(sample(WARPED, transformer, x, y, z), sample(WARPED, transformer, x, y, z + width),
                        at(transformer, "z", z, z + width, x, y));
            }
        }
    }

    @Test
    void horizontalShiftMovesTheSample() {
        Random random = new Random(SEED);
        int moved = 0;
        for (int i = 0; i < SAMPLES; i++) {
            int x = blockIn(random, SQUARE.blockDomain(Direction.Axis.X));
            int y = blockY(random);
            int z = blockIn(random, SQUARE.blockDomain(Direction.Axis.Z));
            if (sample(WARPED, SQUARE, x, y, z) != sample(UNWARPED, SQUARE, x, y, z)) {
                moved++;
            }
        }

        assertTrue(moved >= SAMPLES * MIN_WARPED_SHARE,
                "the horizontal shift moved only " + moved + " of " + SAMPLES + " samples");
    }

    private static double sample(DensityFunction function, WorldFold transformer, int x, int y, int z) {
        DensityFunction.FunctionContext at = new DensityFunction.SinglePointContext(x, y, z);

        return GenerationTransformerContext.withTransformer(transformer, () -> function.compute(at));
    }

    private static String at(WorldFold transformer, String axis, int from, int to, int firstOther, int secondOther) {
        return "shifted_noise in " + transformer + " at " + axis + "=" + from + " vs " + axis + "=" + to
                + " (" + firstOther + ", " + secondOther + ")";
    }
}
