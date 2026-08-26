package com.toroidalworld.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.noise.GenerationTransformerContext.Context;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class ShiftNoiseSlotScalingTest {
    private static final long SEED = 0x510753CA1EL;
    private static final int SAMPLES = 16;
    private static final double COORDINATE_SPAN = 512.0;

    private static final NormalNoise.NoiseParameters PARAMETERS =
            new NormalNoise.NoiseParameters(-6, DoubleArrayList.of(1.0, 1.0, 1.0));

    private static final Holder<NormalNoise.NoiseParameters> NOISE_DATA = Holder.direct(PARAMETERS);

    private static final DensityFunction.NoiseHolder NOISE = new DensityFunction.NoiseHolder(
            NOISE_DATA, NormalNoise.create(new LegacyRandomSource(SEED), PARAMETERS));

    private static final WorldFold SQUARE =
            WorldFolds.of(FlatShape.latticeTorus(new WorldLoopBounds(-16, 16, -16, 16), FlatShape.NO_SKEW));

    private static final SlotAxes NONE_X = new SlotAxes(SlotAxis.NONE, SlotAxis.X, SlotAxis.Z);

    private static final DensityFunctions.ShiftB SHIFT_NOISE =
            (DensityFunctions.ShiftB) withLiveNoise(DensityFunctions.shiftB(NOISE_DATA));

    @Test
    void noneXSlotArrivesAtTheNoiseScaled() {
        Random random = new Random(SEED);
        for (int i = 0; i < SAMPLES; i++) {
            double x = coordinate(random);
            double y = coordinate(random);
            double z = coordinate(random);
            assertEquals(reference(NONE_X, x * NoiseConstants.SHIFT_SCALE, y, z),
                    folded(NONE_X, x, y, z),
                    at("x", x, y, z));
        }
    }

    @Test
    void noneYSlotArrivesAtTheNoiseScaled() {
        Random random = new Random(SEED);
        for (int i = 0; i < SAMPLES; i++) {
            double x = coordinate(random);
            double y = coordinate(random);
            double z = coordinate(random);
            assertEquals(reference(SlotAxes.DEFAULT, x, y * NoiseConstants.SHIFT_SCALE, z),
                    folded(SlotAxes.DEFAULT, x, y, z),
                    at("y", x, y, z));
        }
    }

    @Test
    void noneZSlotArrivesAtTheNoiseScaled() {
        Random random = new Random(SEED);
        for (int i = 0; i < SAMPLES; i++) {
            double x = coordinate(random);
            double y = coordinate(random);
            double z = coordinate(random);
            assertEquals(reference(DensityFunctionSlotAxes.SHIFT_B, x, y, z * NoiseConstants.SHIFT_SCALE),
                    folded(DensityFunctionSlotAxes.SHIFT_B, x, y, z),
                    at("z", x, y, z));
        }
    }

    private static double folded(SlotAxes axes, double x, double y, double z) {
        Context generation = GenerationTransformerContext.context();

        try (Context.BindingScope bindingScope = generation.bind(SQUARE, axes, generation.horizontalScale())) {
            return SHIFT_NOISE.compute(x, y, z);
        }
    }

    private static double reference(SlotAxes axes, double x, double y, double z) {
        Context generation = GenerationTransformerContext.context();

        try (Context.BindingScope bindingScope = generation.bind(SQUARE, axes, generation.horizontalScale())) {
            return ContextScaledNoise.sample(generation, NOISE, x, y, z, NoiseConstants.SHIFT_SCALE)
                    * NoiseConstants.SHIFT_AMPLITUDE;
        }
    }

    private static double coordinate(Random random) {
        return (random.nextDouble() - 0.5) * COORDINATE_SPAN;
    }

    private static String at(String slot, double x, double y, double z) {
        return slot + " slot at (" + x + ", " + y + ", " + z + ")";
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
