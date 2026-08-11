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
import net.minecraft.world.level.levelgen.synth.SimplexNoise;

// The sampler has two contracts and they are not the same one. PERIODICITY — a coordinate and its copy one world width
// away read the same — is bit-exact by the fold-first design and is asserted with no epsilon. CONTINUITY — the field
// has no edge where the two ends of the world meet — is the contract this class exists for, and it is asserted as the
// limit it actually is: the last sliver of the axis and the first block of it are the same physical place, so the two
// samples must converge. Vanilla's own simplex is run through that same measurement to show it does not: that is the
// baseline for the fix, taken without a game round.
//
// A degenerate sampler (a constant) would satisfy every seam assertion while generating a flat field, so the output is
// also pinned to vary, and an unbounded transformer is pinned to reproduce vanilla bit for bit — which is what
// validates the transcription of vanilla's algorithm underneath the wrapping.
class PeriodicSimplexSamplerTest {
    private static final long SEED = 0x0153EL;
    private static final int LINE_SAMPLES = 256;
    private static final double MIN_SPREAD = 0.1;
    private static final int SIXTEENTHS = 16;

    private static final long[] WORLD_SEEDS = {0x0153EL, 0xC0FFEEL, -1234567890123456789L};

    // The scales Biome bakes into the coordinates it hands its three climate noises: 0.05 / 0.025 / 0.0125 are the
    // FROZEN ice-patch octaves, 0.2 and 0.09 the patch-edge lookups, 0.125 the height-adjusted temperature. 1.0 is
    // there because it lands the period on the world width itself.
    private static final double[] SCALES = {0.0125, 0.025, 0.05, 0.09, 0.125, 0.2, 1.0};

    // Where the period is coarse enough for the field to have somewhere to go — the lowest octaves fit only a handful
    // of cells in a lap and are pinned for closure, not for character.
    private static final double[] SPREAD_SCALES = {0.05, 0.2, 1.0};

    // The standard test world (32 chunks, 512 blocks, bounds -256..255), a small one, a rectangle whose two periods
    // are near-coprime — the regime where the shared skew denominator collapses — and one wrapped axis beside an
    // unbounded one.
    private static final WorldLoopTransformer DEFAULT = transformer(-16, 16, -16, 16);
    private static final WorldLoopTransformer SMALL = transformer(-8, 8, -8, 8);
    private static final WorldLoopTransformer UNEVEN = transformer(-32, 32, 0, 16);
    private static final WorldLoopTransformer X_ONLY = new WorldLoopTransformer(
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE));

    private static final List<WorldLoopTransformer> BOTH_AXES = List.of(DEFAULT, SMALL, UNEVEN);
    private static final List<WorldLoopTransformer> WRAPPED_X = List.of(DEFAULT, SMALL, UNEVEN, X_ONLY);

    // A sliver of a block short of the upper bound. The upper bound itself belongs to the far side of the seam, so the
    // limit approached from below is what the first block on that side has to equal.
    private static final double SEAM_EPSILON = 1.0E-9;

    // The field moves by at most its own gradient times that sliver, orders of magnitude under this.
    private static final double CONTINUITY_TOLERANCE = 1.0E-6;

    // What the unwrapped field does at the same place. Any value clear of the noise floor makes the point; the
    // measured gaps run far above it.
    private static final double VANILLA_SEAM_GAP_FLOOR = 0.05;

    // SimplexNoise.getCornerNoise3D's own base, and the number the floored skew exists to keep tight.
    private static final double VANILLA_KERNEL_RADIUS_SQR = 0.5;

    private static WorldLoopTransformer transformer(int xChunkMin, int xChunkMax, int zChunkMin, int zChunkMax) {
        return new WorldLoopTransformer(new WorldLoopBounds(xChunkMin, xChunkMax, zChunkMin, zChunkMax));
    }

    // A real vanilla SimplexNoise beside a replica of its private permutation table, built by replaying the
    // constructor's shuffle on an identically seeded random. The offsets are asserted against the real instance, so a
    // replay that ever drifted from the constructor fails loudly rather than quietly sampling a different field.
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

        // Biome asks all three of its noises with useNoiseStart false, so the offsets stay out of the coordinate; they
        // are carried anyway because the octave walk above this will pass them when a caller does ask for them.
        double sample(WorldLoopTransformer transformer, double scale, double x, double z) {
            return PeriodicSimplexSampler.sample(permutations, 0.0, 0.0, transformer, scale, x, z);
        }
    }

    // Whole sixteenths of a block, uniformly over the axis — dyadic, so the coordinate plus a world width is exact.
    private static double blockInDomain(Random random, WrapDomain domain) {
        return domain.lowerBound + random.nextInt(domain.domainLength) + sixteenth(random);
    }

    // Walks the axis end to end in LINE_SAMPLES strides with a sixteenth jitter inside each, so the whole seam line
    // gets covered rather than a lucky subset. An unbounded axis has no ends to walk between; there the walk covers a
    // plain span around the origin.
    private static double lineCoord(Random random, WrapDomain domain, int step) {
        if (domain instanceof WrapDomain.Noop) {
            return -2048.0 + step * (4096.0 / LINE_SAMPLES) + sixteenth(random);
        }

        return domain.lowerBound + step * ((double) domain.domainLength / LINE_SAMPLES) + sixteenth(random);
    }

    private static double sixteenth(Random random) {
        return random.nextInt(SIXTEENTHS) / (double) SIXTEENTHS;
    }

    private static String at(WorldLoopTransformer transformer, long worldSeed, double scale) {
        return "in " + transformer + " with seed " + worldSeed + " and scale " + scale;
    }

    @Nested
    class SkewDerivation {
        @Test
        void sharesOneDenominatorBetweenTheTwoAxes() {
            assertEquals(26, PeriodicNoiseSampler.period(DEFAULT.coords.x, 0.05));
            assertEquals(26, PeriodicSimplexSampler.skewDenominator(26, 26));
            assertEquals(9, PeriodicSimplexSampler.skewNumerator(26));

            assertEquals(102, PeriodicNoiseSampler.period(DEFAULT.coords.x, 0.2));
            assertEquals(37, PeriodicSimplexSampler.skewNumerator(102));
        }

        // The kernel radius vanilla ships is exactly the squared distance to the nearest corner a cell leaves out, and
        // it stays tight only while the skew sits at or under vanilla's. Flooring is what guarantees that, so the
        // guarantee is asserted rather than trusted: over every denominator a real world can produce, the left-out
        // corner never comes inside the kernel.
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
            long xPeriod = PeriodicNoiseSampler.period(X_ONLY.coords.x, 0.05);
            long zPeriod = PeriodicNoiseSampler.period(X_ONLY.coords.z, 0.05);
            assertEquals(0, zPeriod);
            assertEquals(xPeriod, PeriodicSimplexSampler.skewDenominator(xPeriod, zPeriod));
        }

        // The documented degradation: two periods with no common factor leave nothing to rationalise the skew against,
        // and it collapses to zero — gradient noise on a square lattice, which the seam contracts below still hold on.
        @Test
        void collapsesToZeroWhenThePeriodsAreCoprime() {
            long xPeriod = PeriodicNoiseSampler.period(UNEVEN.coords.x, 0.05);
            long zPeriod = PeriodicNoiseSampler.period(UNEVEN.coords.z, 0.05);
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
            for (WorldLoopTransformer transformer : WRAPPED_X) {
                double period = transformer.coords.x.domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double x = blockInDomain(random, transformer.coords.x);
                            double z = lineCoord(random, transformer.coords.z, i);

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
            for (WorldLoopTransformer transformer : BOTH_AXES) {
                double period = transformer.coords.z.domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double x = lineCoord(random, transformer.coords.x, i);
                            double z = blockInDomain(random, transformer.coords.z);

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
            for (WorldLoopTransformer transformer : BOTH_AXES) {
                double xPeriod = transformer.coords.x.domainLength;
                double zPeriod = transformer.coords.z.domainLength;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double x = blockInDomain(random, transformer.coords.x);
                            double z = blockInDomain(random, transformer.coords.z);

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

    // The contract the card exists for. The last sliver of an axis and its first block are one place, approached from
    // the two sides, so the two samples have to converge — and vanilla's, measured the same way at the same
    // coordinates, does not. That second half is the baseline: it is the reading the old build gives, established
    // without generating a world.
    @Nested
    class SeamContinuity {
        @Test
        void closesAcrossTheXSeamAlongTheWholeLine() {
            Random random = new Random(SEED);
            for (WorldLoopTransformer transformer : WRAPPED_X) {
                WrapDomain xDomain = transformer.coords.x;
                double before = xDomain.upperBound - SEAM_EPSILON;
                double after = xDomain.lowerBound;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double z = lineCoord(random, transformer.coords.z, i);
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
            for (WorldLoopTransformer transformer : BOTH_AXES) {
                WrapDomain zDomain = transformer.coords.z;
                double before = zDomain.upperBound - SEAM_EPSILON;
                double after = zDomain.lowerBound;
                for (long worldSeed : WORLD_SEEDS) {
                    NoiseInstance noise = NoiseInstance.of(worldSeed);
                    for (double scale : SCALES) {
                        for (int i = 0; i < LINE_SAMPLES; i++) {
                            double x = lineCoord(random, transformer.coords.x, i);
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

        // What the same measurement reads on vanilla's own field, sampled the way Biome samples it — the pre-scaled
        // coordinate straight into SimplexNoise, nothing folded. The two sides of the seam are 511 blocks apart there
        // and read as unrelated places, which is the defect.
        @Test
        void vanillaTearsTheSameSeamItIsAskedToClose() {
            Random random = new Random(SEED);
            WrapDomain xDomain = DEFAULT.coords.x;
            double before = xDomain.upperBound - SEAM_EPSILON;
            double after = xDomain.lowerBound;
            for (long worldSeed : WORLD_SEEDS) {
                NoiseInstance noise = NoiseInstance.of(worldSeed);
                for (double scale : SCALES) {
                    double widest = 0.0;
                    for (int i = 0; i < LINE_SAMPLES; i++) {
                        double z = lineCoord(random, DEFAULT.coords.z, i) * scale;
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

    // With nothing wrapping, the sampler is vanilla's algorithm and must come out of it bit for bit — the guard that
    // the periodic machinery is bolted onto a faithful transcription and not onto a lookalike.
    @Nested
    class VanillaParity {
        @Test
        void reproducesVanillaBitForBitOnAnUnboundedTransformer() {
            Random random = new Random(SEED);
            for (long worldSeed : WORLD_SEEDS) {
                NoiseInstance noise = NoiseInstance.of(worldSeed);
                for (double scale : SCALES) {
                    for (int i = 0; i < LINE_SAMPLES; i++) {
                        double x = lineCoord(random, WorldLoopTransformer.NOOP.coords.x, i);
                        double z = lineCoord(random, WorldLoopTransformer.NOOP.coords.z, i);

                        double periodic = noise.sample(WorldLoopTransformer.NOOP, scale, x, z);
                        double vanilla = noise.vanilla().getValue(x * scale, z * scale);
                        assertEquals(vanilla, periodic,
                                () -> "sample(" + x + ", " + z + ") vs vanilla "
                                        + at(WorldLoopTransformer.NOOP, worldSeed, scale));
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
            for (WorldLoopTransformer transformer : WRAPPED_X) {
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

        // Min-to-max over one full sweep: along the seam line X sits at the bound and Z walks the world; around the
        // world X walks a whole lap at a fixed Z.
        private double spread(Random random, NoiseInstance noise, WorldLoopTransformer transformer, double scale,
                boolean alongSeam) {
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            for (int i = 0; i < LINE_SAMPLES; i++) {
                double x = alongSeam ? transformer.coords.x.lowerBound : lineCoord(random, transformer.coords.x, i);
                double z = alongSeam ? lineCoord(random, transformer.coords.z, i) : 5.0;
                double value = noise.sample(transformer, scale, x, z);
                min = Math.min(min, value);
                max = Math.max(max, value);
            }

            return max - min;
        }
    }
}
