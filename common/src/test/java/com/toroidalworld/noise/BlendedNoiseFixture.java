package com.toroidalworld.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.toroidalworld.core.WorldFold;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

final class BlendedNoiseFixture {
    static final int MAIN_OCTAVES = 8;
    static final int LIMIT_OCTAVES = 16;

    private static final double NOISE_MULTIPLIER = 684.412;
    private static final double LIMIT_DIVISOR = 512.0;
    private static final double DENSITY_DIVISOR = 128.0;
    private static final double PERMUTATION_SPAN = 256.0;
    private static final int PERMUTATION_SIZE = 256;
    private static final double UNDECLARED_SHARE = GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE;
    private static final long OCTAVE_SEED_STEP = 0x9E3779B97F4A7C15L;
    private static final long MIN_LIMIT_SALT = 1L;
    private static final long MAX_LIMIT_SALT = 101L;
    private static final long MAIN_SALT = 201L;

    record Params(double xzScale, double yScale, double xzFactor, double yFactor, double smear) {
        static final Params OVERWORLD = new Params(0.25, 0.125, 80.0, 160.0, 8.0);

        double xzMultiplier() {
            return NOISE_MULTIPLIER * this.xzScale;
        }

        double yMultiplier() {
            return NOISE_MULTIPLIER * this.yScale;
        }

        double limitSmear() {
            return yMultiplier() * this.smear;
        }

        double mainScale() {
            return xzMultiplier() / this.xzFactor;
        }
    }

    static long mix(long seed) {
        long mixed = seed;
        mixed = (mixed ^ (mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        mixed = (mixed ^ (mixed >>> 27)) * 0x94D049BB133111EBL;
        return mixed ^ (mixed >>> 31);
    }

    record Octave(ImprovedNoise vanilla, byte[] permutations, double xo, double yo, double zo) {
        static Octave of(long seed) {
            ImprovedNoise vanilla = new ImprovedNoise(new LegacyRandomSource(seed));
            RandomSource random = new LegacyRandomSource(seed);
            double xo = random.nextDouble() * PERMUTATION_SPAN;
            double yo = random.nextDouble() * PERMUTATION_SPAN;
            double zo = random.nextDouble() * PERMUTATION_SPAN;
            byte[] permutations = new byte[PERMUTATION_SIZE];
            for (int i = 0; i < PERMUTATION_SIZE; i++) {
                permutations[i] = (byte) i;
            }

            for (int i = 0; i < PERMUTATION_SIZE; i++) {
                int offset = random.nextInt(PERMUTATION_SIZE - i);
                byte tmp = permutations[i];
                permutations[i] = permutations[i + offset];
                permutations[i + offset] = tmp;
            }

            assertEquals(vanilla.xo, xo);
            assertEquals(vanilla.yo, yo);
            assertEquals(vanilla.zo, zo);
            return new Octave(vanilla, permutations, xo, yo, zo);
        }
    }

    record Replica(Octave[] mainOctaves, Octave[] minOctaves, Octave[] maxOctaves) {
        static Replica of(long seed) {
            Octave[] mainOctaves = new Octave[MAIN_OCTAVES];
            Octave[] minOctaves = new Octave[LIMIT_OCTAVES];
            Octave[] maxOctaves = new Octave[LIMIT_OCTAVES];
            for (int i = 0; i < LIMIT_OCTAVES; i++) {
                minOctaves[i] = Octave.of(mix(seed + OCTAVE_SEED_STEP * (i + MIN_LIMIT_SALT)));
                maxOctaves[i] = Octave.of(mix(seed + OCTAVE_SEED_STEP * (i + MAX_LIMIT_SALT)));
                if (i < MAIN_OCTAVES) {
                    mainOctaves[i] = Octave.of(mix(seed + OCTAVE_SEED_STEP * (i + MAIN_SALT)));
                }
            }
            return new Replica(mainOctaves, minOctaves, maxOctaves);
        }
    }

    @SuppressWarnings("deprecation")
    static double vanilla(Replica replica, Params params, double blockX, double blockY, double blockZ) {
        double limitX = blockX * params.xzMultiplier();
        double limitY = blockY * params.yMultiplier();
        double limitZ = blockZ * params.xzMultiplier();
        double mainX = limitX / params.xzFactor();
        double mainY = limitY / params.yFactor();
        double mainZ = limitZ / params.xzFactor();
        double limitSmear = params.limitSmear();
        double mainSmear = limitSmear / params.yFactor();
        double mainNoiseValue = 0.0;
        double pow = 1.0;
        for (int i = 0; i < MAIN_OCTAVES; i++) {
            Octave octave = replica.mainOctaves()[i];
            mainNoiseValue += octave.vanilla().noise(PerlinNoise.wrap(mainX * pow), PerlinNoise.wrap(mainY * pow),
                    PerlinNoise.wrap(mainZ * pow), mainSmear * pow, mainY * pow) / pow;
            pow /= 2.0;
        }

        double factor = (mainNoiseValue / 10.0 + 1.0) / 2.0;
        boolean isMax = factor >= 1.0;
        boolean isMin = factor <= 0.0;
        double blendMin = 0.0;
        double blendMax = 0.0;
        pow = 1.0;
        for (int i = 0; i < LIMIT_OCTAVES; i++) {
            double wx = PerlinNoise.wrap(limitX * pow);
            double wy = PerlinNoise.wrap(limitY * pow);
            double wz = PerlinNoise.wrap(limitZ * pow);
            double yScalePow = limitSmear * pow;
            if (!isMax) {
                blendMin += replica.minOctaves()[i].vanilla().noise(wx, wy, wz, yScalePow, limitY * pow) / pow;
            }

            if (!isMin) {
                blendMax += replica.maxOctaves()[i].vanilla().noise(wx, wy, wz, yScalePow, limitY * pow) / pow;
            }

            pow /= 2.0;
        }

        return Mth.clampedLerp(factor, blendMin / LIMIT_DIVISOR, blendMax / LIMIT_DIVISOR) / DENSITY_DIVISOR;
    }

    @SuppressWarnings("deprecation")
    static double folded(Replica replica, Params params, WorldFold fold,
            double blockX, double blockY, double blockZ) {
        double limitY = blockY * params.yMultiplier();
        double mainY = limitY / params.yFactor();
        double limitSmear = params.limitSmear();
        double mainSmear = limitSmear / params.yFactor();
        double mainNoiseValue = 0.0;
        double pow = 1.0;
        for (int i = 0; i < MAIN_OCTAVES; i++) {
            Octave octave = replica.mainOctaves()[i];
            mainNoiseValue += sample(octave, fold, params.mainScale() * pow, blockX,
                    PerlinNoise.wrap(mainY * pow), blockZ, mainSmear * pow, mainY * pow) / pow;
            pow /= 2.0;
        }

        double factor = (mainNoiseValue / 10.0 + 1.0) / 2.0;
        boolean isMax = factor >= 1.0;
        boolean isMin = factor <= 0.0;
        double blendMin = 0.0;
        double blendMax = 0.0;
        pow = 1.0;
        for (int i = 0; i < LIMIT_OCTAVES; i++) {
            double wy = PerlinNoise.wrap(limitY * pow);
            double yScalePow = limitSmear * pow;
            double limitScale = params.xzMultiplier() * pow;
            if (!isMax) {
                blendMin += sample(replica.minOctaves()[i], fold, limitScale, blockX, wy, blockZ,
                        yScalePow, limitY * pow) / pow;
            }

            if (!isMin) {
                blendMax += sample(replica.maxOctaves()[i], fold, limitScale, blockX, wy, blockZ,
                        yScalePow, limitY * pow) / pow;
            }

            pow /= 2.0;
        }

        return Mth.clampedLerp(factor, blendMin / LIMIT_DIVISOR, blendMax / LIMIT_DIVISOR) / DENSITY_DIVISOR;
    }

    static double sample(Octave octave, WorldFold fold, double scale,
            double x, double y, double z, double yScale, double yFudge) {
        return sample(octave.permutations(), octave.xo(), octave.yo(), octave.zo(), fold, scale,
                x, y, z, yScale, yFudge, UNDECLARED_SHARE);
    }

    static double sample(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold fold, double scale,
            double x, double y, double z, double yScale, double yFudge, double verticalShare) {
        GenerationTransformerContext.Context context = GenerationTransformerContext.context();

        try (GenerationTransformerContext.Context.ScaleScope scope = context.withScale(scale, verticalShare)) {
            return PeriodicNoiseSampler.sample(permutations, xOffset, yOffset, zOffset, fold, context,
                    x, y, z, yScale, yFudge);
        }
    }

    private BlendedNoiseFixture() {
    }
}
