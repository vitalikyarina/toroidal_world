package com.toroidalworld.noise;

import java.util.List;
import java.util.Random;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.options.WorldLoopBounds;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
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

    public static final WorldLoopTransformer SQUARE =
            new WorldLoopTransformer(new WorldLoopBounds(-16, 16, -16, 16));

    public static final WorldLoopTransformer RECTANGULAR =
            new WorldLoopTransformer(new WorldLoopBounds(-16, 16, -8, 8));

    public static final List<WorldLoopTransformer> WORLDS = List.of(SQUARE, RECTANGULAR);

    public static DensityFunction withLiveNoise(DensityFunction function) {
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

    public static int blockIn(Random random, WrapDomain domain) {
        return domain.lowerBound + random.nextInt(domain.domainLength);
    }

    public static int blockY(Random random) {
        return LOWEST_Y + random.nextInt(WORLD_HEIGHT);
    }

    private DensityFunctionFixture() {
    }
}
