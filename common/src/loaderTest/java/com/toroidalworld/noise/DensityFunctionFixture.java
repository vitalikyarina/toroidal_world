package com.toroidalworld.noise;

import java.util.List;
import java.util.Random;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public final class DensityFunctionFixture {
    public static final long SEED = 0x0153EL;

    private static final int WORLD_HEIGHT = 384;
    private static final int LOWEST_Y = -64;

    private static final NormalNoise.NoiseParameters PARAMETERS =
            new NormalNoise.NoiseParameters(-6, DoubleArrayList.of(1.0, 1.0, 1.0));

    public static final Holder<NormalNoise.NoiseParameters> NOISE_DATA = Holder.direct(PARAMETERS);

    private static final DensityFunction.NoiseHolder NOISE = new DensityFunction.NoiseHolder(
            NOISE_DATA, NormalNoise.create(new LegacyRandomSource(SEED), PARAMETERS));

    public static final int CLIMATE_FIRST_OCTAVE = -10;

    public static final DoubleList CLIMATE_AMPLITUDES = DoubleArrayList.of(1.5, 0.0, 1.0);

    public static final double CLIMATE_XZ_SCALE = 0.25;

    private static final NormalNoise.NoiseParameters CLIMATE_PARAMETERS =
            new NormalNoise.NoiseParameters(CLIMATE_FIRST_OCTAVE, CLIMATE_AMPLITUDES);

    public static final Holder<NormalNoise.NoiseParameters> CLIMATE_NOISE_DATA = Holder.direct(CLIMATE_PARAMETERS);

    private static final DensityFunction.NoiseHolder CLIMATE_NOISE = new DensityFunction.NoiseHolder(
            CLIMATE_NOISE_DATA, NormalNoise.create(new LegacyRandomSource(SEED), CLIMATE_PARAMETERS));

    public static final WorldFold SQUARE =
            WorldFolds.of(FlatShape.latticeTorus(new WorldLoopBounds(-16, 16, -16, 16), FlatShape.NO_SKEW));

    public static final WorldFold RECTANGULAR =
            WorldFolds.of(FlatShape.latticeTorus(new WorldLoopBounds(-16, 16, -8, 8), FlatShape.NO_SKEW));

    public static final List<WorldFold> WORLDS = List.of(SQUARE, RECTANGULAR);

    public static DensityFunction withLiveNoise(DensityFunction function) {
        return withNoise(function, NOISE);
    }

    public static DensityFunction withClimateNoise(DensityFunction function) {
        return withNoise(function, CLIMATE_NOISE);
    }

    private static DensityFunction withNoise(DensityFunction function, DensityFunction.NoiseHolder holder) {
        return function.mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction input) {
                return input;
            }

            @Override
            public DensityFunction.NoiseHolder visitNoise(DensityFunction.NoiseHolder noise) {
                return holder;
            }
        });
    }

    public static int blockIn(Random random, WrapDomain domain) {
        return domain.lowerBound + random.nextInt(domain.domainLength);
    }

    public static int blockY(Random random) {
        return LOWEST_Y + random.nextInt(WORLD_HEIGHT);
    }

    private DensityFunctionFixture() {
    }
}
