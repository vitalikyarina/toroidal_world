package com.toroidalworld.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

class PeriodicSimplexSamplerTest {
    private static final long SEED = 0x0153EL;
    private static final int LINE_SAMPLES = 256;
    private static final double MIN_SPREAD = 0.1;
    private static final int SIXTEENTHS = 16;

    private static final long[] WORLD_SEEDS = {0x0153EL, 0xC0FFEEL, -1234567890123456789L};

    private static final double[] SCALES = {0.0125, 0.025, 0.05, 0.09, 0.125, 0.2, 1.0};

    private static final double[] SPREAD_SCALES = {0.05, 0.2, 1.0};

    private static final WorldFold DEFAULT = transformer(-16, 16, -16, 16);
    private static final WorldFold SMALL = transformer(-8, 8, -8, 8);
    private static final WorldFold UNEVEN = transformer(-32, 32, 0, 16);
    private static final WorldFold X_ONLY = WorldFolds.of(FlatShape.cylinder(
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE)));

    private static final List<WorldFold> BOTH_AXES = List.of(DEFAULT, SMALL, UNEVEN);
    private static final List<WorldFold> WRAPPED_X = List.of(DEFAULT, SMALL, UNEVEN, X_ONLY);

    private static final double SEAM_EPSILON = 1.0E-9;

    private static final double CONTINUITY_TOLERANCE = 1.0E-6;

    private static final double VANILLA_SEAM_GAP_FLOOR = 0.05;

    private static final double VANILLA_KERNEL_RADIUS_SQR = 0.5;

    private static WorldFold transformer(int xChunkMin, int xChunkMax, int zChunkMin, int zChunkMax) {
        return WorldFolds.of(FlatShape.latticeTorus(
                new WorldLoopBounds(xChunkMin, xChunkMax, zChunkMin, zChunkMax), FlatShape.NO_SKEW));
    }

    private record NoiseInstance(SimplexNoise vanilla, int[] permutations, double xo, double zo) {
        static NoiseInstance of(long worldSeed) {
            SimplexNoise vanilla = new SimplexNoise(new LegacyRandomSource(worldSeed));
            RandomSource random = new LegacyRandomSource(worldSeed);
            double xo = random.nextDouble() * 256.0;
            double yo = random.nextDouble() * 256.0;
            double zo = random.nextDouble() * 256.0;
            int[] permutations = new int[256];
            for (int i = 0; i < 256; i++) {
                permutations[i] = i;
            }

            for (int i = 0; i < 256; i++) {
                int offset = random.nextInt(256 - i);
                int tmp = permutations[i];
                permutations[i] = permutations[i + offset];
                permutations[i + offset] = tmp;
            }

            assertEquals(vanilla.xo, xo);
            assertEquals(vanilla.yo, yo);
            assertEquals(vanilla.zo, zo);
            return new NoiseInstance(vanilla, permutations, xo, yo);
        }

        double sample(WorldFold transformer, double scale, double x, double z) {
            return PeriodicSimplexSampler.sample(permutations, 0.0, 0.0, transformer, scale, x, z);
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

    private static String at(WorldFold transformer, long worldSeed, double scale) {
        return "in " + transformer + " with seed " + worldSeed + " and scale " + scale;
    }

    @Nested
    class SkewDerivation {
        @Test
        void sharesOneDenominatorBetweenTheTwoAxes() {
            assertEquals(26, PeriodicNoiseSampler.period(DEFAULT.blockDomain(Direction.Axis.X), 0.05));
            assertEquals(26, PeriodicSimplexSampler.skewDenominator(26, 26));
            assertEquals(9, PeriodicSimplexSampler.skewNumerator(26));

            assertEquals(102, PeriodicNoiseSampler.period(DEFAULT.blockDomain(Direction.Axis.X), 0.2));
            assertEquals(37, PeriodicSimplexSampler.skewNumerator(102));
        }

        @Test
        void keepsTheLeftOutCornerOutsideVanillasKernel() {
            for (long denominator = 1; denominator <= 4096; denominator++) {
                double skew = (double) PeriodicSimplexSampler.skewNumerator(denominator) / denominator;
                double unskew = skew / (1.0 + 2.0 * skew);
                double leftOutCornerSqr = square(1.0 - 2.0 * unskew)
                        / (square(1.0 - unskew) + square(unskew));
                long at = denominator;
                assertTrue(leftOutCornerSqr >= VANILLA_KERNEL_RADIUS_SQR,
                        () -> "left-out corner sits at " + leftOutCornerSqr + " with denominator " + at);
            }
        }

        private static double square(double value) {
            return value * value;
        }

        @Test
        void takesTheLoopedAxisWhenTheOtherIsUnbounded() {
            long xPeriod = PeriodicNoiseSampler.period(X_ONLY.blockDomain(Direction.Axis.X), 0.05);
            long zPeriod = PeriodicNoiseSampler.period(X_ONLY.blockDomain(Direction.Axis.Z), 0.05);
            assertEquals(0, zPeriod);
            assertEquals(xPeriod, PeriodicSimplexSampler.skewDenominator(xPeriod, zPeriod));
        }

        @Test
        void collapsesToZeroWhenThePeriodsAreCoprime() {
            long xPeriod = PeriodicNoiseSampler.period(UNEVEN.blockDomain(Direction.Axis.X), 0.05);
            long zPeriod = PeriodicNoiseSampler.period(UNEVEN.blockDomain(Direction.Axis.Z), 0.05);
            assertEquals(51, xPeriod);
            assertEquals(13, zPeriod);
            assertEquals(1, PeriodicSimplexSampler.skewDenominator(xPeriod, zPeriod));
            assertEquals(0, PeriodicSimplexSampler.skewNumerator(1));
        }

        @Test
        void keepsVanillaSkewWhenNothingWraps() {
            assertEquals(0, PeriodicSimplexSampler.skewDenominator(0, 0));
            assertEquals(0, PeriodicSimplexSampler.skewNumerator(0));
        }
    }

    @Nested
    class XAxis {
        @Test
        void agreesOneWorldWidthApartAlongTheWholeSeamLine() {
            Random random = new Random(SEED);
            for (WorldFold transformer : WRAPPED_X) {
                double period = transformer.blockDomain(Direction.Axis.X).domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double x = blockInDomain(random, transformer.blockDomain(Direction.Axis.X));
                            double z = lineCoord(random, transformer.blockDomain(Direction.Axis.Z), i);

                            double base = noise.sample(transformer, scale, x, z);
                            double lap = noise.sample(transformer, scale, x + period, z);
                            assertEquals(base, lap,
                                    () -> "sample(" + x + ", " + z + ") vs one X lap "
                                            + at(transformer, worldSeed, scale));
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
            for (WorldFold transformer : BOTH_AXES) {
                double period = transformer.blockDomain(Direction.Axis.Z).domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double x = lineCoord(random, transformer.blockDomain(Direction.Axis.X), i);
                            double z = blockInDomain(random, transformer.blockDomain(Direction.Axis.Z));

                            double base = noise.sample(transformer, scale, x, z);
                            double lap = noise.sample(transformer, scale, x, z + period);
                            assertEquals(base, lap,
                                    () -> "sample(" + x + ", " + z + ") vs one Z lap "
                                            + at(transformer, worldSeed, scale));
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
            for (WorldFold transformer : BOTH_AXES) {
                double xPeriod = transformer.blockDomain(Direction.Axis.X).domainLength;
                double zPeriod = transformer.blockDomain(Direction.Axis.Z).domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double x = blockInDomain(random, transformer.blockDomain(Direction.Axis.X));
                            double z = blockInDomain(random, transformer.blockDomain(Direction.Axis.Z));

                            double base = noise.sample(transformer, scale, x, z);
                            double corner = noise.sample(transformer, scale, x + xPeriod, z + zPeriod);
                            assertEquals(base, corner,
                                    () -> "sample(" + x + ", " + z + ") vs the corner lap "
                                            + at(transformer, worldSeed, scale));
                        }
                    }
                }
            }
        }
    }

    @Nested
    class SeamContinuity {
        @Test
        void closesAcrossTheXSeamAlongTheWholeLine() {
            Random random = new Random(SEED);
            for (WorldFold transformer : WRAPPED_X) {
                WrapDomain xDomain = transformer.blockDomain(Direction.Axis.X);
                double before = xDomain.upperBound - SEAM_EPSILON;
                double after = xDomain.lowerBound;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double z = lineCoord(random, transformer.blockDomain(Direction.Axis.Z), i);
                            double gap = Math.abs(noise.sample(transformer, scale, before, z)
                                    - noise.sample(transformer, scale, after, z));
                            assertTrue(gap <= CONTINUITY_TOLERANCE,
                                    () -> "X seam gap at z " + z + " is " + gap + " "
                                            + at(transformer, worldSeed, scale));
                        }
                    }
                }
            }
        }

        @Test
        void closesAcrossTheZSeamAlongTheWholeLine() {
            Random random = new Random(SEED);
            for (WorldFold transformer : BOTH_AXES) {
                WrapDomain zDomain = transformer.blockDomain(Direction.Axis.Z);
                double before = zDomain.upperBound - SEAM_EPSILON;
                double after = zDomain.lowerBound;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double x = lineCoord(random, transformer.blockDomain(Direction.Axis.X), i);
                            double gap = Math.abs(noise.sample(transformer, scale, x, before)
                                    - noise.sample(transformer, scale, x, after));
                            assertTrue(gap <= CONTINUITY_TOLERANCE,
                                    () -> "Z seam gap at x " + x + " is " + gap + " "
                                            + at(transformer, worldSeed, scale));
                        }
                    }
                }
            }
        }

        @Test
        void vanillaTearsTheSameSeamItIsAskedToClose() {
            Random random = new Random(SEED);
            WrapDomain xDomain = DEFAULT.blockDomain(Direction.Axis.X);
            double before = xDomain.upperBound - SEAM_EPSILON;
            double after = xDomain.lowerBound;
            for (long worldSeed : WORLD_SEEDS) {
                NoiseInstance noise = NoiseInstance.of(worldSeed);
                for (double scale : SCALES) {
                    double widest = 0.0;
                    for (int i = 0; i < LINE_SAMPLES; i++) {
                        double z = lineCoord(random, DEFAULT.blockDomain(Direction.Axis.Z), i) * scale;
                        widest = Math.max(widest, Math.abs(noise.vanilla().getValue(before * scale, z)
                                - noise.vanilla().getValue(after * scale, z)));
                    }

                    double measured = widest;
                    assertTrue(measured > VANILLA_SEAM_GAP_FLOOR,
                            () -> "vanilla's widest X seam gap is only " + measured + " "
                                    + at(DEFAULT, worldSeed, scale));
                }
            }
        }
    }

    @Nested
    class VanillaParity {
        @Test
        void reproducesVanillaBitForBitOnAnUnboundedTransformer() {
            Random random = new Random(SEED);
            for (long worldSeed : WORLD_SEEDS) {
                NoiseInstance noise = NoiseInstance.of(worldSeed);
                for (double scale : SCALES) {
                    for (int i = 0; i < LINE_SAMPLES; i++) {
                        double x = lineCoord(random, WorldFolds.NOOP.blockDomain(Direction.Axis.X), i);
                        double z = lineCoord(random, WorldFolds.NOOP.blockDomain(Direction.Axis.Z), i);

                        double periodic = noise.sample(WorldFolds.NOOP, scale, x, z);
                        double vanilla = noise.vanilla().getValue(x * scale, z * scale);
                        assertEquals(vanilla, periodic,
                                () -> "sample(" + x + ", " + z + ") vs vanilla "
                                        + at(WorldFolds.NOOP, worldSeed, scale));
                    }
                }
            }
        }
    }

    @Nested
    class Degeneracy {
        @Test
        void outputVariesAlongTheSeamLineAndAroundTheWorld() {
            Random random = new Random(SEED);
            for (WorldFold transformer : WRAPPED_X) {
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SPREAD_SCALES) {
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

        private double spread(Random random, NoiseInstance noise, WorldFold transformer, double scale,
                boolean alongSeam) {
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (int i = 0; i < LINE_SAMPLES; i++) {
                WrapDomain xDomain = transformer.blockDomain(Direction.Axis.X);
                double x = alongSeam ? xDomain.lowerBound : lineCoord(random, xDomain, i);
                double z = alongSeam ? lineCoord(random, transformer.blockDomain(Direction.Axis.Z), i) : 5.0;
                double value = noise.sample(transformer, scale, x, z);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }

            return max - min;
        }
    }
}
