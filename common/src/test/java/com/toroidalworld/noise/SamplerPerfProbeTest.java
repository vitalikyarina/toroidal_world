package com.toroidalworld.noise;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

// Perf probe for the octave variance correction (floating-terrain fix): per-sample cost of the sampler paths on the
// standard 32-chunk (512-block) world. The pre-fix build is the uncorrected path minus one branch and one multiply,
// so the uncorrected row doubles as the old-build baseline; the corrected rows price the fix — the period-1 row is
// the worst case (table walk every sample), the period-32 row is the fast path every non-degenerate octave takes.
// Report-only (build/reports/sampler-perf-probe.txt): timing asserts would flake with machine load.
class SamplerPerfProbeTest {
    private static final WorldFold WORLD = WorldFolds.of(
            FlatShape.latticeTorus(new WorldLoopBounds(-16, 16, -16, 16), FlatShape.NO_SKEW));

    private static final int WORLD_BLOCKS = 512;
    private static final int SAMPLES = 1 << 20;
    private static final int WARMUP_REPS = 3;
    private static final int TIMED_REPS = 5;

    private ImprovedNoise vanilla;
    private byte[] permutations;
    private double xo;
    private double yo;
    private double zo;

    @Test
    void measuresSamplerPaths() {
        setUpOctave(0x7E5717AB1E5L);

        double periodOneScale = 0.25 / WORLD_BLOCKS;
        double fastPathScale = 32.0 / WORLD_BLOCKS;

        StringBuilder report = new StringBuilder();
        report.append("Sampler perf probe — ").append(SAMPLES).append(" samples/rep, best of ").append(TIMED_REPS)
                .append(" reps after ").append(WARMUP_REPS).append(" warmups, 512-block world\n");
        report.append(measure("vanilla ImprovedNoise.noise",
                (x, z) -> this.vanilla.noise(PerlinNoise.wrap(x * periodOneScale), 0.0,
                        PerlinNoise.wrap(z * periodOneScale), 0.0, 0.0)));
        report.append(measure("wrapped, undeclared (pre-fix path), floored",
                periodOneScale, GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE));
        report.append(measure("wrapped, corrected flat, floored (worst case)", periodOneScale, 0.0));
        report.append(measure("wrapped, corrected live, floored (liveness lookup)", periodOneScale, 1.0));
        report.append(measure("wrapped, corrected, period 32 (fast path)", fastPathScale, 0.0));

        Path out = Path.of("build", "reports", "sampler-perf-probe.txt");
        try {
            Files.createDirectories(out.getParent());
            Files.writeString(out, report.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final DoubleList TEMPERATURE_AMPLITUDES = DoubleArrayList.of(1.5, 0.0, 1.0, 0.0, 0.0, 0.0);

    private static final DoubleList CONTINENTALNESS_AMPLITUDES =
            DoubleArrayList.of(1.0, 1.0, 2.0, 2.0, 2.0, 1.0, 1.0, 1.0, 1.0);

    @Test
    void measuresTheOctaveWalkAgainstItsPreCompressionCost() {
        StringBuilder report = new StringBuilder();
        report.append("Octave walk perf probe — ").append(SAMPLES).append(" samples/rep, best of ")
                .append(TIMED_REPS).append(" reps after ").append(WARMUP_REPS)
                .append(" warmups, 512-block world. An undeclared vertical share is the pre-compression cost:")
                .append(" ClimateScaleCompression.factor returns on its first comparison.")
                .append(System.lineSeparator());

        report.append(measureWalk("temperature ladder, undeclared (pre-compression)", TEMPERATURE_AMPLITUDES, -10,
                GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE));
        report.append(measureWalk("temperature ladder, declared, compressed x6.24", TEMPERATURE_AMPLITUDES, -10,
                0.0));
        report.append(measureWalk("continentalness ladder, undeclared (pre-compression)",
                CONTINENTALNESS_AMPLITUDES, -9, GenerationTransformerContext.UNDECLARED_VERTICAL_SHARE));
        report.append(measureWalk("continentalness ladder, declared, factor exactly 1",
                CONTINENTALNESS_AMPLITUDES, -9, 0.0));

        Path out = Path.of("build", "reports", "octave-walk-perf-probe.txt");
        try {
            Files.createDirectories(out.getParent());
            Files.writeString(out, report.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private String measureWalk(String label, DoubleList amplitudes, int firstOctave, double verticalShare) {
        ImprovedNoise[] levels = new ImprovedNoise[amplitudes.size()];
        for (int i = 0; i < levels.length; i++) {
            if (amplitudes.getDouble(i) != 0.0) {
                levels[i] = new ImprovedNoise(new LegacyRandomSource(0x9E3779B9L + i));
            }
        }

        double lowestFreqInputFactor = Math.pow(2.0, firstOctave);
        return GenerationTransformerContext.withTransformer(WORLD, () -> {
            GenerationTransformerContext.Context context = GenerationTransformerContext.context();
            try (GenerationTransformerContext.Context.ScaleScope scope = context.withScale(0.25, verticalShare)) {
                return measure(label, (x, z) -> PeriodicOctaveSampler.sample(context, levels, amplitudes,
                        lowestFreqInputFactor, 1.0, x, 0.0, z, 0.0, 0.0, false));
            }
        });
    }

    private interface Sampler {
        double sample(double x, double z);
    }

    private String measure(String label, double scale, double verticalShare) {
        GenerationTransformerContext.Context context = GenerationTransformerContext.context();

        try (GenerationTransformerContext.Context.ScaleScope scope = context.withScale(scale, verticalShare)) {
            return measure(label, (x, z) -> PeriodicNoiseSampler.sample(this.permutations, this.xo, this.yo, this.zo,
                    WORLD, context, x, 0.0, z, 0.0, 0.0));
        }
    }

    private String measure(String label, Sampler sampler) {
        double sink = 0.0;
        long best = Long.MAX_VALUE;
        for (int rep = 0; rep < WARMUP_REPS + TIMED_REPS; rep++) {
            long start = System.nanoTime();
            for (int i = 0; i < SAMPLES; i++) {
                double x = -256.0 + (i & 0x1FF);
                double z = -256.0 + ((i >>> 9) & 0x1FF);
                sink += sampler.sample(x, z);
            }
            long elapsed = System.nanoTime() - start;
            if (rep >= WARMUP_REPS && elapsed < best) {
                best = elapsed;
            }
        }
        assertTrue(Double.isFinite(sink));
        return String.format("  %-52s %6.2f ns/sample%n", label, (double) best / SAMPLES);
    }

    // The same permutation-table replay as PeriodicNoiseSamplerTest — a real vanilla octave beside the byte table the
    // periodic sampler reads.
    private void setUpOctave(long seed) {
        this.vanilla = new ImprovedNoise(new LegacyRandomSource(seed));
        RandomSource random = new LegacyRandomSource(seed);
        this.xo = random.nextDouble() * 256.0;
        this.yo = random.nextDouble() * 256.0;
        this.zo = random.nextDouble() * 256.0;
        byte[] table = new byte[256];
        for (int i = 0; i < 256; i++) {
            table[i] = (byte) i;
        }
        for (int i = 0; i < 256; i++) {
            int offset = random.nextInt(256 - i);
            byte tmp = table[i];
            table[i] = table[i + offset];
            table[i + offset] = tmp;
        }
        this.permutations = table;
        assertTrue(Arrays.equals(new double[] {this.vanilla.xo, this.vanilla.yo, this.vanilla.zo},
                new double[] {this.xo, this.yo, this.zo}));
    }
}
