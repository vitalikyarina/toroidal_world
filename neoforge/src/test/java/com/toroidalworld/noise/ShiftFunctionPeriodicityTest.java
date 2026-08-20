package com.toroidalworld.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.options.WorldLoopBounds;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class ShiftFunctionPeriodicityTest {
    private static final long SEED = 0x0153EL;
    private static final int SAMPLES = 64;
    private static final int WORLD_HEIGHT = 384;
    private static final int LOWEST_Y = -64;
    private static final double MIN_SPREAD = 0.1;

    private static final NormalNoise.NoiseParameters PARAMETERS =
            new NormalNoise.NoiseParameters(-6, DoubleArrayList.of(1.0, 1.0, 1.0));

    private static final Holder<NormalNoise.NoiseParameters> NOISE_DATA = Holder.direct(PARAMETERS);

    private static final DensityFunction.NoiseHolder NOISE = new DensityFunction.NoiseHolder(
            NOISE_DATA, NormalNoise.create(new LegacyRandomSource(SEED), PARAMETERS));

    private static final WorldLoopTransformer SQUARE =
            new WorldLoopTransformer(new WorldLoopBounds(-16, 16, -16, 16));

    private static final WorldLoopTransformer RECTANGULAR =
            new WorldLoopTransformer(new WorldLoopBounds(-16, 16, -8, 8));

    private static final List<WorldLoopTransformer> WORLDS = List.of(SQUARE, RECTANGULAR);

    private record ShiftFunction(String name, DensityFunction function) {
    }

    private static final List<ShiftFunction> SHIFTS = List.of(
            new ShiftFunction("shift", withLiveNoise(DensityFunctions.shift(NOISE_DATA))),
            new ShiftFunction("shift_a", withLiveNoise(DensityFunctions.shiftA(NOISE_DATA))),
            new ShiftFunction("shift_b", withLiveNoise(DensityFunctions.shiftB(NOISE_DATA))));

    @Test
    void everyShiftFunctionAgreesOneWorldWidthApartInX() {
        Random random = new Random(SEED);
        for (WorldLoopTransformer transformer : WORLDS) {
            int width = transformer.coords.x.domainLength;
            for (ShiftFunction shift : SHIFTS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = blockIn(random, transformer.coords.x);
                    int y = blockY(random);
                    int z = blockIn(random, transformer.coords.z);
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
        for (WorldLoopTransformer transformer : WORLDS) {
            int width = transformer.coords.z.domainLength;
            for (ShiftFunction shift : SHIFTS) {
                for (int i = 0; i < SAMPLES; i++) {
                    int x = blockIn(random, transformer.coords.x);
                    int y = blockY(random);
                    int z = blockIn(random, transformer.coords.z);
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
                double value = sample(shift, SQUARE, blockIn(random, SQUARE.coords.x), blockY(random),
                        blockIn(random, SQUARE.coords.z));
                lowest = Math.min(lowest, value);
                highest = Math.max(highest, value);
            }

            assertTrue(highest - lowest > MIN_SPREAD,
                    shift.name() + " sampled a near-constant field, spread " + (highest - lowest));
        }
    }

    private static double sample(ShiftFunction shift, WorldLoopTransformer transformer, int x, int y, int z) {
        DensityFunction.FunctionContext at = new DensityFunction.SinglePointContext(x, y, z);

        return GenerationTransformerContext.withTransformer(transformer, () -> shift.function().compute(at));
    }

    private static int blockIn(Random random, WrapDomain domain) {
        return domain.lowerBound + random.nextInt(domain.domainLength);
    }

    private static int blockY(Random random) {
        return LOWEST_Y + random.nextInt(WORLD_HEIGHT);
    }

    private static String at(ShiftFunction shift, WorldLoopTransformer transformer,
            String axis, int from, int to, int firstOther, int secondOther) {
        return shift.name() + " in " + transformer + " at " + axis + "=" + from + " vs " + axis + "=" + to
                + " (" + firstOther + ", " + secondOther + ")";
    }

    private static DensityFunction withLiveNoise(DensityFunction function) {
        return function.mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction input) {
                return input;
            }

            @Override
            public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder noise) {
                return NOISE;
            }
        });
    }
}
