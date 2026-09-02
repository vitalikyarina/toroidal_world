package com.toroidalworld.compat.c2me;

import static com.toroidalworld.noise.DensityFunctionFixture.CLIMATE_NOISE_DATA;
import static com.toroidalworld.noise.DensityFunctionFixture.CLIMATE_XZ_SCALE;
import static com.toroidalworld.noise.DensityFunctionFixture.SEED;
import static com.toroidalworld.noise.DensityFunctionFixture.SQUARE;
import static com.toroidalworld.noise.DensityFunctionFixture.blockIn;
import static com.toroidalworld.noise.DensityFunctionFixture.blockY;
import static com.toroidalworld.noise.DensityFunctionFixture.withClimateNoise;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Random;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.ishland.c2me.opts.dfc.common.gen.jvm.AbstractCompiledDensityFunction;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeGen;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

class C2meCompiledDensityFunctionTest {
    private static final int SAMPLES = 16;

    private static final double SHIFT_X = 3.0;
    private static final double SHIFT_Z = -5.0;

    private record Points(int[] xs, int[] ys, int[] zs) implements DensityFunction.ContextProvider {
        static Points over(WorldFold fold, int count) {
            Random random = new Random(SEED);
            int[] xs = new int[count];
            int[] ys = new int[count];
            int[] zs = new int[count];

            for (int i = 0; i < count; i++) {
                xs[i] = blockIn(random, fold.blockDomain(Direction.Axis.X));
                ys[i] = blockY(random);
                zs[i] = blockIn(random, fold.blockDomain(Direction.Axis.Z));
            }

            return new Points(xs, ys, zs);
        }

        @Override
        public DensityFunction.FunctionContext forIndex(int index) {
            return new DensityFunction.SinglePointContext(this.xs[index], this.ys[index], this.zs[index]);
        }

        @Override
        public void fillAllDirectly(double[] output, DensityFunction function) {
            for (int i = 0; i < output.length; i++) {
                output[i] = function.compute(this.forIndex(i));
            }
        }
    }

    @Test
    void theCompiledClimateFunctionComputesLikeTheVanillaPath() {
        DensityFunction source = climateSource();
        DensityFunction compiled = compile(source, SQUARE);
        Points points = Points.over(SQUARE, SAMPLES);

        for (int i = 0; i < SAMPLES; i++) {
            DensityFunction.FunctionContext at = points.forIndex(i);
            double vanilla = GenerationTransformerContext.withTransformer(SQUARE, () -> source.compute(at));
            double emitted = GenerationTransformerContext.withTransformer(SQUARE, () -> compiled.compute(at));

            assertEquals(vanilla, emitted, "at (" + at.blockX() + ", " + at.blockY() + ", " + at.blockZ() + ")");
        }
    }

    @Test
    void theCompiledClimateFunctionFillsAnArrayLikeTheVanillaPath() {
        DensityFunction source = climateSource();
        DensityFunction compiled = compile(source, SQUARE);
        Points points = Points.over(SQUARE, SAMPLES);
        double[] vanilla = new double[SAMPLES];
        double[] emitted = new double[SAMPLES];

        GenerationTransformerContext.runWithTransformer(SQUARE, () -> source.fillArray(vanilla, points));
        GenerationTransformerContext.runWithTransformer(SQUARE, () -> compiled.fillArray(emitted, points));

        assertArrayEquals(vanilla, emitted);
    }

    private static DensityFunction climateSource() {
        return withClimateNoise(DensityFunctions.shiftedNoise2d(
                DensityFunctions.constant(SHIFT_X), DensityFunctions.constant(SHIFT_Z),
                CLIMATE_XZ_SCALE, CLIMATE_NOISE_DATA));
    }

    private static DensityFunction compile(DensityFunction source, WorldFold fold) {
        DensityFunction compiled = GenerationTransformerContext.withRouterBuild(fold, () -> {
            BytecodeGen.Context context = BytecodeGen.initContext();
            DensityFunction function = context.compileDelayed("toroidal_parity", source);
            BytecodeGen.finalizeCompilation(context);
            return function;
        });

        assertInstanceOf(AbstractCompiledDensityFunction.class, compiled,
                "C2ME did not compile the function, so the emitted path is not what this test samples");

        return compiled;
    }
}
