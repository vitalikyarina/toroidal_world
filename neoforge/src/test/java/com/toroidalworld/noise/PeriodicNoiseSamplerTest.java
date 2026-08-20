package com.toroidalworld.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;

class PeriodicNoiseSamplerTest {
    private static final long SEED = 0x0153EL;
    private static final int LINE_SAMPLES = 256;
    private static final double MIN_SPREAD = 0.1;
    private static final int SIXTEENTHS = 16;

    private static final long[] WORLD_SEEDS = {0x0153EL, 0xC0FFEEL, -1234567890123456789L};

    private static final double[] SCALES = {0.25, 1.0, 100.0, 1.17};

    private static final double[] PARITY_SCALES = {0.25, 1.0, 100.0};

    private static final double[][] Y_PARAMS = {{0.0, 0.0}, {1.0, 2.0}};

    private static final WorldLoopTransformer EVEN = transformer(-32, 32, -32, 32);
    private static final WorldLoopTransformer ODD = transformer(-2, 3, -2, 3);
    private static final WorldLoopTransformer UNEVEN = transformer(-48, 16, 0, 16);
    private static final WorldLoopTransformer X_ONLY = new WorldLoopTransformer(
            new WorldLoopBounds(new AxisBounds.Looped(-32, 32), AxisBounds.Unbounded.INSTANCE));

    private static final List<WorldLoopTransformer> BOTH_AXES = List.of(EVEN, ODD, UNEVEN);
    private static final List<WorldLoopTransformer> WRAPPED_X = List.of(EVEN, ODD, UNEVEN, X_ONLY);

    private static WorldLoopTransformer transformer(int xChunkMin, int xChunkMax, int zChunkMin, int zChunkMax) {
        return new WorldLoopTransformer(new WorldLoopBounds(xChunkMin, xChunkMax, zChunkMin, zChunkMax));
    }

    private record NoiseInstance(ImprovedNoise vanilla, byte[] permutations, double xo, double yo, double zo) {
        static NoiseInstance of(long worldSeed) {
            ImprovedNoise vanilla = new ImprovedNoise(new LegacyRandomSource(worldSeed));
            RandomSource random = new LegacyRandomSource(worldSeed);
            double xo = random.nextDouble() * 256.0;
            double yo = random.nextDouble() * 256.0;
            double zo = random.nextDouble() * 256.0;
            byte[] permutations = new byte[256];
            for (int i = 0; i < 256; i++) {
                permutations[i] = (byte) i;
            }

            for (int i = 0; i < 256; i++) {
                int offset = random.nextInt(256 - i);
                byte tmp = permutations[i];
                permutations[i] = permutations[i + offset];
                permutations[i + offset] = tmp;
            }

            assertEquals(vanilla.xo, xo);
            assertEquals(vanilla.yo, yo);
            assertEquals(vanilla.zo, zo);
            return new NoiseInstance(vanilla, permutations, xo, yo, zo);
        }

        double sample(WorldLoopTransformer transformer, double scale,
                double x, double y, double z, double yScale, double yFudge) {
            return sample(transformer, SlotAxes.DEFAULT, scale, x, y, z, yScale, yFudge);
        }

        double sample(WorldLoopTransformer transformer, SlotAxes axes, double scale,
                double x, double y, double z, double yScale, double yFudge) {
            GenerationTransformerContext.Context context = GenerationTransformerContext.context();

            try (GenerationTransformerContext.Context.SlotAxesScope _ = context.withSlotAxes(axes);
                    GenerationTransformerContext.Context.ScaleScope _ = context.withScale(scale)) {
                return PeriodicNoiseSampler.sample(permutations, xo, yo, zo, transformer, context,
                        x, y, z, yScale, yFudge);
            }
        }
    }

    private static double blockInDomain(Random random, WrapDomain domain) {
        return domain.lowerBound + random.nextInt(domain.domainLength) + sixteenth(random);
    }

    private static double lineCoord(Random random, WrapDomain domain, int step) {
        if (domain instanceof WrapDomain.Noop) {
            return -2048.0 + step * (4096.0 / LINE_SAMPLES) + sixteenth(random);
        }

        return domain.lowerBound + step * ((double) domain.domainLength / LINE_SAMPLES) + sixteenth(random);
    }

    private static double sixteenth(Random random) {
        return random.nextInt(SIXTEENTHS) / (double) SIXTEENTHS;
    }

    private static double sampleY(Random random) {
        return random.nextInt(384) - 64 + random.nextDouble();
    }

    private static String at(WorldLoopTransformer transformer, long worldSeed, double scale) {
        return "in " + transformer + " with seed " + worldSeed + " and scale " + scale;
    }

    @Nested
    class PeriodDerivation {
        @Test
        void roundsClampsAndPassesUnboundedThrough() {
            WrapDomain evenX = EVEN.coords.x;
            assertEquals(256, PeriodicNoiseSampler.period(evenX, 0.25));
            assertEquals(1024, PeriodicNoiseSampler.period(evenX, 1.0));
            assertEquals(102400, PeriodicNoiseSampler.period(evenX, 100.0));
            assertEquals(94, PeriodicNoiseSampler.period(ODD.coords.x, 1.17));
            assertEquals(1, PeriodicNoiseSampler.period(evenX, 1.0 / 2048.0));
            assertEquals(0, PeriodicNoiseSampler.period(X_ONLY.coords.z, 1.0));
        }
    }

    @Nested
    class XAxis {
        @Test
        void agreesOneWorldWidthApartAlongTheWholeSeamLine() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : WRAPPED_X) {
                double period = transformer.coords.x.domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (double[] yParams : Y_PARAMS) {
                            for (int i = 0; i < LINE_SAMPLES; i++) {
                                double x = blockInDomain(random, transformer.coords.x);
                                double y = sampleY(random);
                                double z = lineCoord(random, transformer.coords.z, i);

                                double base = noise.sample(transformer, scale, x, y, z, yParams[0], yParams[1]);
                                double lap = noise.sample(transformer, scale, x + period, y, z, yParams[0], yParams[1]);
                                assertEquals(base, lap,
                                        () -> "sample(" + x + ", " + y + ", " + z + ") vs one X lap "
                                                + at(transformer, worldSeed, scale));
                            }
                        }
                    }
                }
            }
        }
    }

    @Nested
    class ZAxis {
        @Test
        void agreesOneWorldWidthApartAlongTheWholeSeamLine() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : BOTH_AXES) {
                double period = transformer.coords.z.domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (double[] yParams : Y_PARAMS) {
                            for (int i = 0; i < LINE_SAMPLES; i++) {
                                double x = lineCoord(random, transformer.coords.x, i);
                                double y = sampleY(random);
                                double z = blockInDomain(random, transformer.coords.z);

                                double base = noise.sample(transformer, scale, x, y, z, yParams[0], yParams[1]);
                                double lap = noise.sample(transformer, scale, x, y, z + period, yParams[0], yParams[1]);
                                assertEquals(base, lap,
                                        () -> "sample(" + x + ", " + y + ", " + z + ") vs one Z lap "
                                                + at(transformer, worldSeed, scale));
                            }
                        }
                    }
                }
            }
        }
    }

    @Nested
    class Corner {
        @Test
        void agreesWhenBothAxesWrapAtOnce() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : BOTH_AXES) {
                double xPeriod = transformer.coords.x.domainLength;
                double zPeriod = transformer.coords.z.domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (double[] yParams : Y_PARAMS) {
                            for (int i = 0; i < LINE_SAMPLES; i++) {
                                double x = blockInDomain(random, transformer.coords.x);
                                double y = sampleY(random);
                                double z = blockInDomain(random, transformer.coords.z);

                                double base = noise.sample(transformer, scale, x, y, z, yParams[0], yParams[1]);
                                double corner = noise.sample(transformer, scale,
                                        x + xPeriod, y, z + zPeriod, yParams[0], yParams[1]);
                                assertEquals(base, corner,
                                        () -> "sample(" + x + ", " + y + ", " + z + ") vs the corner lap "
                                                + at(transformer, worldSeed, scale));
                            }
                        }
                    }
                }
            }
        }
    }

    @Nested
    class VanillaParity {
        @Test
        @SuppressWarnings("deprecation")
        void reproducesVanillaBitForBitWhenThePeriodIsAMultipleOf256() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : List.of(EVEN, X_ONLY)) {
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : PARITY_SCALES) {
                        for (double[] yParams : Y_PARAMS) {
                            for (int i = 0; i < LINE_SAMPLES; i++) {
                                double x = blockInDomain(random, transformer.coords.x);
                                double y = sampleY(random);
                                double z = lineCoord(random, transformer.coords.z, i);

                                double periodic = noise.sample(transformer, scale, x, y, z, yParams[0], yParams[1]);
                                double vanilla = noise.vanilla().noise(x * scale, y, z * scale, yParams[0], yParams[1]);
                                assertEquals(vanilla, periodic,
                                        () -> "sample(" + x + ", " + y + ", " + z + ") vs vanilla "
                                                + at(transformer, worldSeed, scale));
                            }
                        }
                    }
                }
            }
        }
    }

    @Nested
    class LowestPeriod {
        private static final double TINY_SCALE = 1.0 / 2048.0;

        @Test
        void closesTheLapAtPeriodOne() {
            Random random = new Random(SEED);
            double period = EVEN.coords.x.domainLength;
            for (long worldSeed : WORLD_SEEDS) {
                NoiseInstance noise = NoiseInstance.of(worldSeed);
                for (int i = 0; i < LINE_SAMPLES; i++) {
                    double x = blockInDomain(random, EVEN.coords.x);
                    double y = sampleY(random);
                    double z = blockInDomain(random, EVEN.coords.z);

                    double base = noise.sample(EVEN, TINY_SCALE, x, y, z, 0.0, 0.0);
                    double lap = noise.sample(EVEN, TINY_SCALE, x + period, y, z, 0.0, 0.0);
                    assertTrue(Double.isFinite(base),
                            () -> "sample(" + x + ", " + y + ", " + z + ") is not finite at period 1");
                    assertEquals(base, lap,
                            () -> "sample(" + x + ", " + y + ", " + z + ") vs one X lap at period 1");
                }
            }
        }
    }

    @Nested
    class Degeneracy {
        @Test
        void outputVariesAlongTheSeamLineAndAroundTheWorld() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : WRAPPED_X) {
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        double alongSeam = spread(random, noise, transformer, scale, true);
                        assertTrue(alongSeam >= MIN_SPREAD,
                                () -> "spread along the seam line is " + alongSeam + " "
                                        + at(transformer, worldSeed, scale));

                        double aroundWorld = spread(random, noise, transformer, scale, false);
                        assertTrue(aroundWorld >= MIN_SPREAD,
                                () -> "spread around the world is " + aroundWorld + " "
                                        + at(transformer, worldSeed, scale));
                    }
                }
            }
        }

        private double spread(Random random, NoiseInstance noise, WorldLoopTransformer transformer, double scale,
                boolean alongSeam) {
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            double y = sampleY(random);
            for (int i = 0; i < LINE_SAMPLES; i++) {
                double x = alongSeam ? transformer.coords.x.lowerBound : lineCoord(random, transformer.coords.x, i);
                double z = alongSeam ? lineCoord(random, transformer.coords.z, i) : 5.0;
                double value = noise.sample(transformer, scale, x, y, z, 0.0, 0.0);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
            return max - min;
        }
    }
}
