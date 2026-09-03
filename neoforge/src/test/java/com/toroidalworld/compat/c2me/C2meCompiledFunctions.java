package com.toroidalworld.compat.c2me;

import static com.toroidalworld.noise.DensityFunctionFixture.CLIMATE_NOISE_DATA;
import static com.toroidalworld.noise.DensityFunctionFixture.CLIMATE_XZ_SCALE;
import static com.toroidalworld.noise.DensityFunctionFixture.SEED;
import static com.toroidalworld.noise.DensityFunctionFixture.blockIn;
import static com.toroidalworld.noise.DensityFunctionFixture.blockY;
import static com.toroidalworld.noise.DensityFunctionFixture.withClimateNoise;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Random;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.ishland.c2me.opts.dfc.common.gen.jvm.AbstractCompiledDensityFunction;
import com.ishland.c2me.opts.dfc.common.gen.jvm.BytecodeGen;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

final class C2meCompiledFunctions {
    private static final double SHIFT_X = 3.0;
    private static final double SHIFT_Z = -5.0;

    record Points(int[] xs, int[] ys, int[] zs) implements DensityFunction.ContextProvider {
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

    static DensityFunction climateSource() {
        return withClimateNoise(DensityFunctions.shiftedNoise2d(
                DensityFunctions.constant(SHIFT_X), DensityFunctions.constant(SHIFT_Z),
                CLIMATE_XZ_SCALE, CLIMATE_NOISE_DATA));
    }

    static DensityFunction compileFolded(String name, DensityFunction source, WorldFold fold) {
        return compile(name, source, fold);
    }

    static DensityFunction compileUnfolded(String name, DensityFunction source) {
        return compile(name, source, null);
    }

    private static DensityFunction compile(String name, DensityFunction source, @Nullable WorldFold fold) {
        DensityFunction compiled = GenerationTransformerContext.withRouterBuild(fold, () -> {
            BytecodeGen.Context context = BytecodeGen.initContext();
            DensityFunction function = context.compileDelayed(name, source);
            BytecodeGen.finalizeCompilation(context);
            return function;
        });

        assertInstanceOf(AbstractCompiledDensityFunction.class, compiled,
                "C2ME did not compile the function, so the emitted path is not what this test samples");

        return compiled;
    }

    private C2meCompiledFunctions() {
    }
}
