package com.toroidalworld.compat.c2me;

import static com.toroidalworld.noise.DensityFunctionFixture.SQUARE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.toroidalworld.noise.GenerationTransformerContext;

import net.minecraft.world.level.levelgen.DensityFunction;

class C2meCompiledDensityFunctionTest {
    private static final int SAMPLES = 16;

    @Test
    void theCompiledClimateFunctionComputesLikeTheVanillaPath() {
        DensityFunction source = C2meCompiledFunctions.climateSource();
        DensityFunction compiled = C2meCompiledFunctions.compileFolded("toroidal_parity", source, SQUARE);
        C2meCompiledFunctions.Points points = C2meCompiledFunctions.Points.over(SQUARE, SAMPLES);

        for (int i = 0; i < SAMPLES; i++) {
            DensityFunction.FunctionContext at = points.forIndex(i);
            double vanilla = GenerationTransformerContext.withTransformer(SQUARE, () -> source.compute(at));
            double emitted = GenerationTransformerContext.withTransformer(SQUARE, () -> compiled.compute(at));

            assertEquals(vanilla, emitted, "at (" + at.blockX() + ", " + at.blockY() + ", " + at.blockZ() + ")");
        }
    }

    @Test
    void theCompiledClimateFunctionFillsAnArrayLikeTheVanillaPath() {
        DensityFunction source = C2meCompiledFunctions.climateSource();
        DensityFunction compiled = C2meCompiledFunctions.compileFolded("toroidal_parity", source, SQUARE);
        C2meCompiledFunctions.Points points = C2meCompiledFunctions.Points.over(SQUARE, SAMPLES);
        double[] vanilla = new double[SAMPLES];
        double[] emitted = new double[SAMPLES];

        GenerationTransformerContext.runWithTransformer(SQUARE, () -> source.fillArray(vanilla, points));
        GenerationTransformerContext.runWithTransformer(SQUARE, () -> compiled.fillArray(emitted, points));

        assertArrayEquals(vanilla, emitted);
    }
}
