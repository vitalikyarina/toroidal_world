package com.toroidalworld.noise;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.toroidalworld.noise.BlendedNoiseFixture.mix;
import static com.toroidalworld.noise.BlendedNoiseFixture.sample;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.noise.BlendedNoiseFixture.Octave;
import com.toroidalworld.noise.BlendedNoiseFixture.Params;
import com.toroidalworld.noise.BlendedNoiseFixture.Replica;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

// Measurement harness and regression gate of the floating-terrain fix: compares the value distribution of the
// biome-parameter fields between vanilla sampling and the periodic fold, on the standard 32-chunk (512-block) world,
// at quart (4-block) resolution. Five variants per field: vanilla with the domain warp, vanilla without it, the
// wrapped sampling with no correction (the defect), a wrapped+periodic-warp prototype, and the wrapped sampling with
// the octave variance correction switched on (the fix). Writes a plain-text report under build/reports; the
// assertions pin the replica against vanilla and finiteness, and gate the damp-only fix: damped octaves (table
// k < 1) land on the vanilla window rms, every other octave is bit-exactly untouched, the ridge peaks-and-valleys
// spike-zone share stays in vanilla's neighborhood (the amplifying v1 doubled it and failed in-game), and a pooled
// field-variance band checks the composition.
class FieldDistributionProbeTest {
    private record NoiseParams(String name, int firstOctave, double[] amplitudes) {
        int spanOctaves() {
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            for (int i = 0; i < amplitudes.length; i++) {
                if (amplitudes[i] != 0.0) {
                    min = Math.min(min, i);
                    max = Math.max(max, i);
                }
            }
            return max - min;
        }

        // Vanilla NormalNoise: 0.16666… / expectedDeviation(last nonzero − first nonzero),
        // expectedDeviation(k) = 0.1 · (1 + 1/(k+1)).
        double valueFactor() {
            double expectedDeviation = 0.1 * (1.0 + 1.0 / (spanOctaves() + 1));
            return 0.16666666666666666 / expectedDeviation;
        }
    }

    // Verified against data/minecraft/worldgen/noise/*.json in the 1.21.1 client jar and NoiseData in the 26.2
    // sources (identical).
    private static final NoiseParams CONTINENTALNESS =
            new NoiseParams("continentalness", -9, new double[] {1, 1, 2, 2, 2, 1, 1, 1, 1});
    private static final NoiseParams EROSION = new NoiseParams("erosion", -9, new double[] {1, 1, 0, 1, 1});
    private static final NoiseParams OFFSET = new NoiseParams("offset", -3, new double[] {1, 1, 1, 0});
    private static final NoiseParams RIDGE = new NoiseParams("ridge", -7, new double[] {1, 2, 1, 0, 0, 0});

    private static final double XZ_SCALE = 0.25;
    private static final double DETUNE = NoiseConstants.SECOND_LAYER_DETUNE;
    private static final int QUART = 4;
    private static final int WORLD_BLOCKS = 512;
    private static final int GRID = WORLD_BLOCKS / QUART;
    private static final int MIN_BLOCK = -256;

    private static final WorldFold WORLD = WorldFolds.of(
            FlatShape.torus(new WorldLoopBounds(-16, 16, -16, 16)));

    private static final long[] WORLD_SEEDS = {
            0x0153EL, 0xC0FFEEL, -1234567890123456789L, 42L, 8180061971476006269L, -8011452369450108867L,
            0xDEADBEEFL, 31337L, -777L, 0x1CEB00DAL, 987654321987654321L, -424242424242L
    };



    private enum WalkMode { CURRENT, CORRECTED }

    // Two-layer NormalNoise replica over real vanilla octaves. The vanilla side samples the genuine ImprovedNoise
    // instances; the wrapped side runs the same octave walk the mod's mixins run (context scale = base · octave
    // factor, raw coordinates) through PeriodicNoiseSampler with the replica tables.
    private static final class LayeredNoise {
        private final NoiseParams params;
        private final Octave[][] layers;
        private final double valueFactor;

        LayeredNoise(long worldSeed, int salt, NoiseParams params) {
            this.params = params;
            this.valueFactor = params.valueFactor();
            this.layers = new Octave[2][params.amplitudes.length];
            for (int layer = 0; layer < 2; layer++) {
                for (int i = 0; i < params.amplitudes.length; i++) {
                    if (params.amplitudes[i] != 0.0) {
                        long seed = mix(worldSeed + 0x9E3779B97F4A7C15L * (salt * 64L + layer * 32L + i + 1L));
                        this.layers[layer][i] = Octave.of(seed);
                    }
                }
            }
        }

        // x/z already carry the caller's horizontal scale (and warp), exactly like vanilla NormalNoise.getValue input.
        @SuppressWarnings("deprecation")
        double vanillaValue(double x, double z) {
            return (vanillaLayer(0, x, z) + vanillaLayer(1, x * DETUNE, z * DETUNE)) * this.valueFactor;
        }

        @SuppressWarnings("deprecation")
        private double vanillaLayer(int layer, double x, double z) {
            double inputFactor = Math.pow(2.0, this.params.firstOctave);
            double valueWeight = lowestFreqValueFactor();
            double sum = 0.0;
            for (int i = 0; i < this.params.amplitudes.length; i++) {
                Octave octave = this.layers[layer][i];
                if (octave != null) {
                    sum += this.params.amplitudes[i] * valueWeight
                            * octave.vanilla().noise(PerlinNoise.wrap(x * inputFactor), 0.0,
                                    PerlinNoise.wrap(z * inputFactor), 0.0, 0.0);
                }

                inputFactor *= 2.0;
                valueWeight /= 2.0;
            }
            return sum;
        }

        // x/z are raw block coordinates; baseScale is what the caller would park in the generation context.
        double wrappedValue(double x, double z, double baseScale) {
            return wrappedValue(x, z, baseScale, WalkMode.CURRENT);
        }

        // The fix under test: the same octave walk with the sampler's damp-only variance correction switched on,
        // exactly as ImprovedNoiseMixin runs it for a vertically-flat router field.
        double correctedValue(double x, double z, double baseScale) {
            return wrappedValue(x, z, baseScale, WalkMode.CORRECTED);
        }

        double wrappedValue(double x, double z, double baseScale, WalkMode mode) {
            return (wrappedLayer(0, x, z, baseScale, mode) + wrappedLayer(1, x, z, baseScale * DETUNE, mode))
                    * this.valueFactor;
        }

        private double wrappedLayer(int layer, double x, double z, double baseScale, WalkMode mode) {
            double inputFactor = Math.pow(2.0, this.params.firstOctave);
            double valueWeight = lowestFreqValueFactor();
            double sum = 0.0;
            for (int i = 0; i < this.params.amplitudes.length; i++) {
                Octave octave = this.layers[layer][i];
                if (octave != null) {
                    sum += this.params.amplitudes[i] * valueWeight
                            * sample(octave.permutations(), octave.xo(), octave.yo(),
                                    octave.zo(), WORLD, baseScale * inputFactor,
                                    x, 0.0, z, 0.0, 0.0, mode == WalkMode.CORRECTED ? 0.0 : -1.0);
                }

                inputFactor *= 2.0;
                valueWeight /= 2.0;
            }
            return sum;
        }

        private double lowestFreqValueFactor() {
            int n = this.params.amplitudes.length;
            return Math.pow(2.0, n - 1) / (Math.pow(2.0, n) - 1.0);
        }
    }

    private record Stats(double mean, double std, double min, double max, double p05, double p95, double rough) {
        static Stats of(double[][] field) {
            int n = field.length * field[0].length;
            double sum = 0.0;
            double min = Double.MAX_VALUE;
            double max = -Double.MAX_VALUE;
            double[] flat = new double[n];
            int k = 0;
            for (double[] row : field) {
                for (double value : row) {
                    sum += value;
                    min = Math.min(min, value);
                    max = Math.max(max, value);
                    flat[k++] = value;
                }
            }
            double mean = sum / n;

            double varSum = 0.0;
            for (double value : flat) {
                varSum += (value - mean) * (value - mean);
            }

            Arrays.sort(flat);
            double p05 = flat[(int) (n * 0.05)];
            double p95 = flat[(int) (n * 0.95)];

            // Neighbor-delta deviation along +X at quart stride — the "texture" the depth/factor splines actually see.
            double roughSum = 0.0;
            int roughCount = 0;
            for (int i = 0; i + 1 < field.length; i++) {
                for (int j = 0; j < field[i].length; j++) {
                    double delta = field[i + 1][j] - field[i][j];
                    roughSum += delta * delta;
                    roughCount++;
                }
            }

            return new Stats(mean, Math.sqrt(varSum / n), min, max, p05, p95, Math.sqrt(roughSum / roughCount));
        }

        String line(String label) {
            return String.format("  %-9s mean=%+.4f std=%.4f min=%+.4f max=%+.4f p05=%+.4f p95=%+.4f roughQ=%.5f",
                    label, mean, std, min, max, p05, p95, rough);
        }
    }

    private final List<double[]> octaveGateRows = new ArrayList<>();
    private final List<double[]> cylinderGateRows = new ArrayList<>();
    private final List<Double> livenessGateRatios = new ArrayList<>();
    private final List<Double> meanSpreadGateRatios = new ArrayList<>();
    private double pooledVanillaVariance;
    private double pooledUncorrectedVariance;
    private double pooledCorrectedVariance;
    private double ridgePeakShareVanilla;
    private double ridgePeakShareCorrected;

    @Test
    void measuresFieldDistributions() {
        StringBuilder report = new StringBuilder();
        report.append("Field distribution probe — 32-chunk (512-block) world, quart grid ")
                .append(GRID).append("x").append(GRID).append(", y=0\n");
        report.append("Variants: van_warp (vanilla, domain warp), van_flat (vanilla, no warp), ")
                .append("mod_cur (periodic, uncorrected), mod_warp (periodic + periodic warp prototype), ")
                .append("mod_fix (periodic + octave variance correction)\n\n");

        appendOctaveTable(report, CONTINENTALNESS);
        appendOctaveTable(report, EROSION);
        report.append('\n');

        appendDegenerateCalibration(report);
        appendMeanSpreadCalibration(report);
        appendRectangularCalibration(report);
        appendCylinderCalibration(report);
        appendVerticalCalibration(report);
        appendBlendedFoldCalibration(report);
        appendExtremeStatistics(report);

        for (long worldSeed : WORLD_SEEDS) {
            report.append("world seed ").append(worldSeed).append('\n');
            LayeredNoise offset = new LayeredNoise(worldSeed, 0, OFFSET);
            measureField(report, worldSeed, 1, CONTINENTALNESS, offset);
            measureField(report, worldSeed, 2, EROSION, offset);
        }

        double pooledCorrectedRatio = Math.sqrt(this.pooledCorrectedVariance / this.pooledVanillaVariance);
        report.append(String.format("pooled variance ratio vs vanilla over %d fields: uncorrected %.3f, corrected %.3f%n",
                WORLD_SEEDS.length * 2,
                Math.sqrt(this.pooledUncorrectedVariance / this.pooledVanillaVariance),
                pooledCorrectedRatio));

        Path out = Path.of("build", "reports", "field-distribution-probe.txt");
        try {
            Files.createDirectories(out.getParent());
            Files.writeString(out, report.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // The regression gate, asserted after the report is on disk so a failing run still leaves the numbers to
        // read. Everything is deterministic — fixed seeds, fixed windows — so the bands cannot flake. Three teeth:
        // damped octaves (table k < 1) must land on the vanilla window rms; every other octave must be untouched
        // (re-introducing the amplification that doubled the ridge spike zone shows up here); and the ridge
        // peaks-and-valleys spike-zone share must stay in vanilla's neighborhood (the artifact-facing metric — the
        // amplifying v1 measured 13.8% against vanilla's 7.6% and failed in-game). The pooled band is a composition
        // check for gross mis-application.
        for (double[] gateRow : this.octaveGateRows) {
            double fraction = gateRow[0];
            double uncorrectedRatio = gateRow[1];
            double correctedRatio = gateRow[2];
            if (OctaveVarianceCorrection.flat(fraction) < 1.0) {
                assertTrue(Math.abs(correctedRatio - 1.0) <= 0.06,
                        "damped octave rms drifted off the vanilla window at f=" + fraction + ": " + correctedRatio);
            } else {
                assertTrue(Math.abs(correctedRatio - uncorrectedRatio) <= 1.0E-9,
                        "correction is not a no-op at f=" + fraction + ": " + correctedRatio);
            }
        }
        assertTrue(Math.abs(this.ridgePeakShareCorrected - this.ridgePeakShareVanilla) <= 3.0,
                "ridge pv>0.7 share drifted off vanilla: " + this.ridgePeakShareCorrected
                        + "% vs " + this.ridgePeakShareVanilla + "%");
        for (double[] cylinderRow : this.cylinderGateRows) {
            double stripRatio = cylinderRow[2] / cylinderRow[1];
            assertTrue(Math.abs(stripRatio - 1.0) <= 0.06,
                    "cylinder strip rms drifted off vanilla at f=" + cylinderRow[0] + ": " + stripRatio);
            assertTrue(cylinderRow[3] == 0.0,
                    "a starved octave still varies around the ring at f=" + cylinderRow[0] + ": " + cylinderRow[3]);
        }
        for (double livenessRatio : this.livenessGateRatios) {
            assertTrue(Math.abs(livenessRatio - 1.0) <= 0.08,
                    "liveness-corrected octave rms drifted off the vanilla window: " + livenessRatio);
        }
        for (double meanSpreadRatio : this.meanSpreadGateRatios) {
            assertTrue(meanSpreadRatio > 0.7 && meanSpreadRatio < 1.3,
                    "corrected world-mean spread drifted off vanilla: " + meanSpreadRatio);
        }
        assertTrue(pooledCorrectedRatio > 0.9 && pooledCorrectedRatio < 1.1,
                "corrected field composition drifted, pooled variance ratio " + pooledCorrectedRatio);
    }

    private void appendOctaveTable(StringBuilder report, NoiseParams params) {
        report.append("octaves of ").append(params.name()).append(" at xzScale ").append(XZ_SCALE)
                .append(" on ").append(WORLD_BLOCKS).append(" blocks:\n");
        for (int i = 0; i < params.amplitudes().length; i++) {
            if (params.amplitudes()[i] == 0.0) {
                continue;
            }
            double octaveScale = XZ_SCALE * Math.pow(2.0, params.firstOctave() + i);
            double trueCells = WORLD_BLOCKS * octaveScale;
            long period = PeriodicNoiseSampler.period(WORLD.blockDomain(Direction.Axis.X), octaveScale,
                    LapFloor.of(WORLD));
            report.append(String.format("  oct %d amp %.1f: true %.2f cells/lap -> period %d%s%n",
                    params.firstOctave() + i, params.amplitudes()[i], trueCells, period,
                    period != Math.round(trueCells * 1e9) / 1e9 && trueCells < 1.0 ? "  (degenerate)" : ""));
        }
    }

    // For a single octave whose true period is f cells per lap (f < 1): the in-world deviation vanilla shows over a
    // world-sized window versus what the period-1 fold shows. Averaged as variance, not std — the folded wave's
    // per-realization std has a wide spread, and matching mean std leaves the ensemble variance (the actual field
    // distribution) well above vanilla's; sqrt of the variance ratio is the attenuation that matches it exactly.
    @SuppressWarnings("deprecation")
    private void appendDegenerateCalibration(StringBuilder report) {
        double[] fractions = {0.125, 0.1875, 0.25, 0.3125, 0.375, 0.4375, 0.5, 0.625, 0.75, 0.875, 1.0, 1.125, 1.25,
                1.375, 1.4375, 1.5, 1.75, 2.0, 2.25, 2.5, 3.0, 4.0, 6.0, 8.0};
        int seeds = 2048;
        int grid = 32;
        java.util.Random windows = new java.util.Random(0x0153E);
        report.append("degenerate-octave calibration (single octave, 512-block lap, ")
                .append(seeds).append(" seeds x ").append(grid).append("x").append(grid).append(" grid):\n");
        for (double fraction : fractions) {
            double scale = fraction / WORLD_BLOCKS;
            double vanillaSum = 0.0;
            double wrappedSum = 0.0;
            double correctedSum = 0.0;
            for (int s = 0; s < seeds; s++) {
                Octave octave = Octave.of(mix(0xCA11B7A7EL + s * 7919L));
                double windowX = windows.nextDouble() * 1.0E6;
                double windowZ = windows.nextDouble() * 1.0E6;
                double[] vanillaGrid = new double[grid * grid];
                double[] wrappedGrid = new double[grid * grid];
                double[] correctedGrid = new double[grid * grid];
                int k = 0;
                for (int i = 0; i < grid; i++) {
                    double x = i * (WORLD_BLOCKS / (double) grid);
                    for (int j = 0; j < grid; j++) {
                        double z = j * (WORLD_BLOCKS / (double) grid);
                        vanillaGrid[k] = octave.vanilla().noise(PerlinNoise.wrap((windowX + x) * scale), 0.0,
                                PerlinNoise.wrap((windowZ + z) * scale), 0.0, 0.0);
                        wrappedGrid[k] = sample(octave.permutations(), octave.xo(), octave.yo(),
                                octave.zo(), WORLD, scale, x - 256.0, 0.0, z - 256.0, 0.0, 0.0, -1.0);
                        correctedGrid[k] = sample(octave.permutations(), octave.xo(),
                                octave.yo(), octave.zo(), WORLD, scale, x - 256.0, 0.0, z - 256.0, 0.0, 0.0, 0.0);
                        k++;
                    }
                }
                vanillaSum += variance(vanillaGrid);
                wrappedSum += variance(wrappedGrid);
                correctedSum += variance(correctedGrid);
            }
            double uncorrectedRatio = Math.sqrt(vanillaSum / wrappedSum);
            double correctedRatio = Math.sqrt(vanillaSum / correctedSum);
            this.octaveGateRows.add(new double[] {fraction, uncorrectedRatio, correctedRatio});
            report.append(String.format(
                    "  f=%.3f cells/lap: vanilla rms std %.4f, floored rms std %.4f, ratio k=%.3f, corrected %.3f%n",
                    fraction, Math.sqrt(vanillaSum / seeds), Math.sqrt(wrappedSum / seeds),
                    uncorrectedRatio, correctedRatio));
        }
        report.append('\n');
    }

    // The production flat table extended with the regime bound: from f=1.5 the quantized period leaves period-1 and
    // the true ratio sits at 1, which the scoring candidates need when a combined f crosses the bound.
    private double k1d(double f) {
        return f >= 1.5 ? 1.0 : OctaveVarianceCorrection.flat(f);
    }

    // Rectangular worlds: the X lap stays 512 blocks, the Z lap grows, so one octave carries a different f per axis.
    // Measures the true vanilla-window/wrapped std ratio per (fx, fz) and scores three scalar-policy candidates
    // against it: k1d of the geometric-mean f, k1d of the min f, and the geometric mean of the per-axis k1d values
    // (the last one is the factorized model: k(f,f) = r(f)^2 with r the one-axis attenuation).
    @SuppressWarnings("deprecation")
    private void appendRectangularCalibration(StringBuilder report) {
        double[] baseFractions = {0.25, 0.5, 0.75, 1.0, 1.25};
        int[] zChunkSizes = {48, 64, 96, 128};
        int seeds = 1024;
        int grid = 32;
        java.util.Random windows = new java.util.Random(0x0153E);
        report.append("rectangular-world calibration (X lap 512 blocks fixed, ").append(seeds)
                .append(" seeds x ").append(grid).append("x").append(grid).append(" grid):\n");
        for (double fx : baseFractions) {
            for (int zChunks : zChunkSizes) {
                double zBlocks = zChunks * 16.0;
                double scale = fx / WORLD_BLOCKS;
                double fz = zBlocks * scale;
                WorldFold world = WorldFolds.of(FlatShape.torus(new WorldLoopBounds(-16, 16, -zChunks / 2, zChunks / 2)));
                double vanillaSum = 0.0;
                double wrappedSum = 0.0;
                for (int s = 0; s < seeds; s++) {
                    Octave octave = Octave.of(mix(0xCA11B7A7EL + s * 7919L));
                    double windowX = windows.nextDouble() * 1.0E6;
                    double windowZ = windows.nextDouble() * 1.0E6;
                    double[] vanillaGrid = new double[grid * grid];
                    double[] wrappedGrid = new double[grid * grid];
                    int k = 0;
                    for (int i = 0; i < grid; i++) {
                        double x = i * (WORLD_BLOCKS / (double) grid);
                        for (int j = 0; j < grid; j++) {
                            double z = j * (zBlocks / grid);
                            vanillaGrid[k] = octave.vanilla().noise(PerlinNoise.wrap((windowX + x) * scale), 0.0,
                                    PerlinNoise.wrap((windowZ + z) * scale), 0.0, 0.0);
                            wrappedGrid[k] = sample(octave.permutations(), octave.xo(),
                                    octave.yo(), octave.zo(), world, scale, x - 256.0, 0.0, z - zBlocks / 2.0,
                                    0.0, 0.0, -1.0);
                            k++;
                        }
                    }
                    vanillaSum += variance(vanillaGrid);
                    wrappedSum += variance(wrappedGrid);
                }
                double measured = Math.sqrt(vanillaSum / wrappedSum);
                report.append(String.format(
                        "  fx=%.3f fz=%.3f (Z %d chunks): k2D=%.3f | geo-f %.3f, min-f %.3f, geo-k %.3f%n",
                        fx, fz, zChunks, measured,
                        k1d(Math.sqrt(fx * fz)), k1d(Math.min(fx, fz)), Math.sqrt(k1d(fx) * k1d(fz))));
            }
        }
        report.append('\n');
    }

    @SuppressWarnings("deprecation")
    private void appendCylinderCalibration(StringBuilder report) {
        double[] fractions = {0.125, 0.25, 0.375, 0.5, 0.75, 1.0, 1.25, 1.4375};
        int seeds = 1024;
        int gridX = 16;
        int gridZ = 256;
        double zSpan = 8192.0;
        WorldFold cylinder = WorldFolds.of(FlatShape.cylinder(WorldLoopBounds.ofWidth(Direction.Axis.X, 32)));
        LapFloor[] floors = {LapFloor.FOUR_CELLS, LapFloor.of(cylinder)};
        java.util.Random windows = new java.util.Random(0x0153E);
        report.append("cylinder calibration (X lap 512 blocks, Z unbounded, window ").append((int) zSpan)
                .append(" blocks of Z, ").append(seeds).append(" seeds x ").append(gridX).append("x").append(gridZ)
                .append(" grid; rms of strip / line / X-step; the last column is the fold's own rule):\n");
        for (double fraction : fractions) {
            double scale = fraction / WORLD_BLOCKS;
            double[][] sums = new double[floors.length + 1][3];
            for (int s = 0; s < seeds; s++) {
                Octave octave = Octave.of(mix(0xCA11B7A7EL + s * 7919L));
                double windowX = windows.nextDouble() * 1.0E6;
                double windowZ = windows.nextDouble() * 1.0E6;
                double[][][] grids = new double[floors.length + 1][gridX][gridZ];
                for (int i = 0; i < gridX; i++) {
                    double x = i * (WORLD_BLOCKS / (double) gridX);
                    for (int j = 0; j < gridZ; j++) {
                        double z = j * (zSpan / gridZ);
                        grids[0][i][j] = octave.vanilla().noise(PerlinNoise.wrap((windowX + x) * scale), 0.0,
                                PerlinNoise.wrap((windowZ + z) * scale), 0.0, 0.0);
                        for (int f = 0; f < floors.length; f++) {
                            grids[f + 1][i][j] = sample(octave.permutations(), octave.xo(), octave.yo(),
                                    octave.zo(), cylinder, scale, x - 256.0, 0.0, windowZ + z, 0.0, 0.0, -1.0,
                                    floors[f]);
                        }
                    }
                }
                for (int v = 0; v < grids.length; v++) {
                    accumulateStrip(grids[v], sums[v]);
                }
            }
            double[] fold = sums[floors.length];
            this.cylinderGateRows.add(new double[] {
                    fraction, Math.sqrt(sums[0][0] / seeds), Math.sqrt(fold[0] / seeds), Math.sqrt(fold[2] / seeds)});
            StringBuilder line = new StringBuilder(String.format("  f=%.4f cells/lap:", fraction));
            appendStripReadings(line, "vanilla", sums[0], seeds);
            for (int f = 0; f < floors.length; f++) {
                appendStripReadings(line, floors[f].name().toLowerCase(java.util.Locale.ROOT), sums[f + 1], seeds);
            }
            report.append(line).append('\n');
        }
        report.append('\n');
    }

    private void accumulateStrip(double[][] grid, double[] sums) {
        int gridX = grid.length;
        int gridZ = grid[0].length;
        double[] flat = new double[gridX * gridZ];
        double lineVariance = 0.0;
        double stepSum = 0.0;
        int k = 0;
        for (int i = 0; i < gridX; i++) {
            lineVariance += variance(grid[i]);
            for (int j = 0; j < gridZ; j++) {
                flat[k++] = grid[i][j];
                if (i + 1 < gridX) {
                    double step = grid[i + 1][j] - grid[i][j];
                    stepSum += step * step;
                }
            }
        }
        sums[0] += variance(flat);
        sums[1] += lineVariance / gridX;
        sums[2] += stepSum / ((gridX - 1) * gridZ);
    }

    private void appendStripReadings(StringBuilder line, String label, double[] sums, int seeds) {
        line.append(String.format("  %s %.4f/%.4f/%.5f", label,
                Math.sqrt(sums[0] / seeds), Math.sqrt(sums[1] / seeds), Math.sqrt(sums[2] / seeds)));
    }

    // 3D noises (cave/spaghetti family) sample Y as a live axis while the horizontal axes fold. Measures how the
    // square-world period-1 deficit k changes when the sampled window extends ν cells vertically: at ν=0 this must
    // reproduce the k1d table; if a live Y restores the variance the way a live horizontal axis does (see the
    // rectangular section), the 2D correction must NOT apply to vertically-sampled noises as-is.
    @SuppressWarnings("deprecation")
    private void appendVerticalCalibration(StringBuilder report) {
        double[] fractions = {0.125, 0.25, 0.5, 0.75, 1.0, 1.25};
        double[] verticalCells = {0.0, 0.25, 0.5, 1.0, 2.0, 4.0, 8.0, 16.0};
        int seeds = 512;
        int gridXZ = 16;
        int gridY = 8;
        java.util.Random windows = new java.util.Random(0x0153E);
        report.append("vertical-liveness calibration (512-block square lap, ").append(seeds)
                .append(" seeds x ").append(gridXZ).append("x").append(gridXZ).append("x").append(gridY)
                .append(" grid, nu = vertical cells in window):\n");
        for (double fraction : fractions) {
            double scale = fraction / WORLD_BLOCKS;
            StringBuilder line = new StringBuilder(String.format("  f=%.3f cells/lap:", fraction));
            for (double nu : verticalCells) {
                int ySteps = nu == 0.0 ? 1 : gridY;
                // The share that makes the sampler's nominal-height ν equal this column's window ν, gating the
                // interpolated liveness relief against the very grid it was measured on.
                double verticalShare = nu / (384.0 * scale);
                double vanillaSum = 0.0;
                double wrappedSum = 0.0;
                double correctedSum = 0.0;
                for (int s = 0; s < seeds; s++) {
                    Octave octave = Octave.of(mix(0xCA11B7A7EL + s * 7919L));
                    double windowX = windows.nextDouble() * 1.0E6;
                    double windowZ = windows.nextDouble() * 1.0E6;
                    double windowY = windows.nextDouble() * 256.0;
                    double[] vanillaGrid = new double[gridXZ * gridXZ * ySteps];
                    double[] wrappedGrid = new double[gridXZ * gridXZ * ySteps];
                    double[] correctedGrid = new double[gridXZ * gridXZ * ySteps];
                    int k = 0;
                    for (int i = 0; i < gridXZ; i++) {
                        double x = i * (WORLD_BLOCKS / (double) gridXZ);
                        for (int j = 0; j < gridXZ; j++) {
                            double z = j * (WORLD_BLOCKS / (double) gridXZ);
                            for (int t = 0; t < ySteps; t++) {
                                double y = windowY + t * (nu / gridY);
                                vanillaGrid[k] = octave.vanilla().noise(PerlinNoise.wrap((windowX + x) * scale),
                                        PerlinNoise.wrap(y), PerlinNoise.wrap((windowZ + z) * scale), 0.0, 0.0);
                                wrappedGrid[k] = sample(octave.permutations(), octave.xo(),
                                        octave.yo(), octave.zo(), WORLD, scale, x - 256.0, y, z - 256.0, 0.0, 0.0,
                                        -1.0);
                                correctedGrid[k] = sample(octave.permutations(), octave.xo(),
                                        octave.yo(), octave.zo(), WORLD, scale, x - 256.0, y, z - 256.0, 0.0, 0.0,
                                        verticalShare);
                                k++;
                            }
                        }
                    }
                    vanillaSum += variance(vanillaGrid);
                    wrappedSum += variance(wrappedGrid);
                    correctedSum += variance(correctedGrid);
                }
                double correctedRatio = Math.sqrt(vanillaSum / correctedSum);
                this.livenessGateRatios.add(correctedRatio);
                line.append(String.format("  nu=%.2f k=%.3f c=%.3f", nu, Math.sqrt(vanillaSum / wrappedSum),
                        correctedRatio));
            }
            report.append(line).append('\n');
        }
        report.append('\n');
    }

    // The base 3D terrain noise (BlendedNoise, overworld params verified against NoiseRouterData: xzScale 0.25,
    // yScale 0.125, xzFactor 80, yFactor 160, smear 8): its dominant low octaves fold at NATURAL periods on the
    // 512-block world (f = 2.67 → period 3, f = 5.35 → period 5) — the band no correction covers — and the in-game
    // per-noise trace named it the floating-island carrier (base_3d ≈ +0.24…+0.39 at every suspended site while
    // vanilla finalDensity never crosses 0 aloft). This measures the full blended walk, vanilla window vs the
    // period-quantized fold, at the two altitudes the in-game sweep used: pooled mean/std, the positive tail shares
    // P(v > 0.2) and P(v > 0.3), the across-seed spread of the slice mean, and the mean per-seed maximum.
    private void appendBlendedFoldCalibration(StringBuilder report) {
        int seeds = 256;
        int grid = 32;
        int[] ySlices = {100, 150};
        java.util.Random windows = new java.util.Random(0x0153E);
        report.append("blended-noise fold calibration (full walk, 512-block lap, ").append(seeds)
                .append(" seeds x ").append(grid).append("x").append(grid).append(" grid):\n");
        for (int ySlice : ySlices) {
            double[] vanillaSums = new double[2];
            double[] foldedSums = new double[2];
            long[] vanillaTails = new long[2];
            long[] foldedTails = new long[2];
            double[] vanillaSeedMeans = new double[seeds];
            double[] foldedSeedMeans = new double[seeds];
            double vanillaMaxSum = 0.0;
            double foldedMaxSum = 0.0;
            long points = 0;
            for (int s = 0; s < seeds; s++) {
                Replica replica = Replica.of(0xB1E4DEDL + s * 7919L);
                double windowX = windows.nextDouble() * 1.0E6;
                double windowZ = windows.nextDouble() * 1.0E6;
                double vanillaSeedSum = 0.0;
                double foldedSeedSum = 0.0;
                double vanillaSeedMax = -Double.MAX_VALUE;
                double foldedSeedMax = -Double.MAX_VALUE;
                for (int i = 0; i < grid; i++) {
                    double x = i * (WORLD_BLOCKS / (double) grid);
                    for (int j = 0; j < grid; j++) {
                        double z = j * (WORLD_BLOCKS / (double) grid);
                        double vanilla = BlendedNoiseFixture.vanilla(replica, Params.OVERWORLD, windowX + x, ySlice, windowZ + z);
                        double folded = BlendedNoiseFixture.folded(replica, Params.OVERWORLD, WORLD, x - 256.0, ySlice, z - 256.0);
                        vanillaSums[0] += vanilla;
                        vanillaSums[1] += vanilla * vanilla;
                        foldedSums[0] += folded;
                        foldedSums[1] += folded * folded;
                        if (vanilla > 0.2) {
                            vanillaTails[0]++;
                        }
                        if (vanilla > 0.3) {
                            vanillaTails[1]++;
                        }
                        if (folded > 0.2) {
                            foldedTails[0]++;
                        }
                        if (folded > 0.3) {
                            foldedTails[1]++;
                        }
                        vanillaSeedSum += vanilla;
                        foldedSeedSum += folded;
                        vanillaSeedMax = Math.max(vanillaSeedMax, vanilla);
                        foldedSeedMax = Math.max(foldedSeedMax, folded);
                        points++;
                    }
                }
                vanillaSeedMeans[s] = vanillaSeedSum / (grid * grid);
                foldedSeedMeans[s] = foldedSeedSum / (grid * grid);
                vanillaMaxSum += vanillaSeedMax;
                foldedMaxSum += foldedSeedMax;
            }
            report.append(String.format(
                    "  y=%d vanilla: mean=%+.4f std=%.4f tail02=%.3f%% tail03=%.3f%% mean_spread=%.4f avg_max=%.4f%n",
                    ySlice, vanillaSums[0] / points,
                    Math.sqrt(vanillaSums[1] / points - Math.pow(vanillaSums[0] / points, 2)),
                    100.0 * vanillaTails[0] / points, 100.0 * vanillaTails[1] / points,
                    Math.sqrt(variance(vanillaSeedMeans)), vanillaMaxSum / seeds));
            report.append(String.format(
                    "  y=%d folded:  mean=%+.4f std=%.4f tail02=%.3f%% tail03=%.3f%% mean_spread=%.4f avg_max=%.4f%n",
                    ySlice, foldedSums[0] / points,
                    Math.sqrt(foldedSums[1] / points - Math.pow(foldedSums[0] / points, 2)),
                    100.0 * foldedTails[0] / points, 100.0 * foldedTails[1] / points,
                    Math.sqrt(variance(foldedSeedMeans)), foldedMaxSum / seeds));
        }
        report.append('\n');
    }

    // Artifacts live in the distribution shape, not the ensemble rms: the v1 round proved that amplifying the
    // near-cell closed waves doubles the ridge pv spike zone even while every extreme quantile stays under vanilla.
    // This measures both — per-realization max |field − world mean| quantiles across seeds, and for ridge the pooled
    // share of the world inside the peaks-and-valleys spike zone — for vanilla, the uncorrected fold and the
    // damp-only correction.
    private void appendExtremeStatistics(StringBuilder report) {
        int seeds = 128;
        int grid = 64;
        report.append("field extreme statistics (max |field - world mean|, ").append(seeds)
                .append(" seeds x ").append(grid).append("x").append(grid).append(" grid, 512-block world):\n");
        for (NoiseParams params : new NoiseParams[] {CONTINENTALNESS, EROSION, RIDGE}) {
            double[] vanilla = new double[seeds];
            double[] uncorrected = new double[seeds];
            double[] corrected = new double[seeds];
            double[] vanillaWorldMeans = new double[seeds];
            double[] correctedWorldMeans = new double[seeds];
            long[] peakZone = new long[3];
            long pooled = 0;
            for (int s = 0; s < seeds; s++) {
                long worldSeed = mix(0x5EED5EED5L + s * 0x9E3779B97F4A7C15L);
                LayeredNoise field = new LayeredNoise(worldSeed, 1, params);
                double[] vanillaGrid = new double[grid * grid];
                double[] uncorrectedGrid = new double[grid * grid];
                double[] correctedGrid = new double[grid * grid];
                int k = 0;
                for (int i = 0; i < grid; i++) {
                    double x = MIN_BLOCK + i * (WORLD_BLOCKS / (double) grid);
                    for (int j = 0; j < grid; j++) {
                        double z = MIN_BLOCK + j * (WORLD_BLOCKS / (double) grid);
                        vanillaGrid[k] = field.vanillaValue(x * XZ_SCALE, z * XZ_SCALE);
                        uncorrectedGrid[k] = field.wrappedValue(x, z, XZ_SCALE, WalkMode.CURRENT);
                        correctedGrid[k] = field.wrappedValue(x, z, XZ_SCALE, WalkMode.CORRECTED);
                        k++;
                    }
                }
                vanilla[s] = maxAbsDeviation(vanillaGrid);
                uncorrected[s] = maxAbsDeviation(uncorrectedGrid);
                corrected[s] = maxAbsDeviation(correctedGrid);
                vanillaWorldMeans[s] = mean(vanillaGrid);
                correctedWorldMeans[s] = mean(correctedGrid);
                if (params == RIDGE) {
                    peakZone[0] += countPeakZone(vanillaGrid);
                    peakZone[1] += countPeakZone(uncorrectedGrid);
                    peakZone[2] += countPeakZone(correctedGrid);
                    pooled += vanillaGrid.length;
                }
            }
            double vanillaMeanSpread = Math.sqrt(variance(vanillaWorldMeans));
            double correctedMeanSpread = Math.sqrt(variance(correctedWorldMeans));
            this.meanSpreadGateRatios.add(correctedMeanSpread / vanillaMeanSpread);
            report.append(' ').append(params.name()).append(":\n");
            report.append(quantileLine("vanilla", vanilla));
            report.append(quantileLine("uncorrected", uncorrected));
            report.append(quantileLine("corrected", corrected));
            report.append(String.format("  world-mean spread: vanilla %.4f, corrected %.4f (ratio %.3f)%n",
                    vanillaMeanSpread, correctedMeanSpread, correctedMeanSpread / vanillaMeanSpread));
            if (params == RIDGE) {
                this.ridgePeakShareVanilla = 100.0 * peakZone[0] / pooled;
                this.ridgePeakShareCorrected = 100.0 * peakZone[2] / pooled;
                report.append(String.format(
                        "  pv>0.7 share (jagged peak zone): vanilla %.2f%%, uncorrected %.2f%%, corrected %.2f%%%n",
                        this.ridgePeakShareVanilla, 100.0 * peakZone[1] / pooled, this.ridgePeakShareCorrected));
            }
        }
        report.append('\n');
    }

    // The DC component: vanilla's window MEAN wanders seed to seed (one world is ocean, another inland), while a
    // damped fold's world mean collapses to ~0 — every toroidal world parks at the spline's coast band. Measures the
    // across-seed spread of the vanilla window mean, the floored fold's world mean, and a fixed-lattice-point anchor
    // sample per octave frequency; the anchor gain a(f) that restores the vanilla mean spread follows as
    // sqrt(vanillaMeanStd² − (k·foldMeanStd)²) / anchorStd.
    @SuppressWarnings("deprecation")
    private void appendMeanSpreadCalibration(StringBuilder report) {
        double[] fractions = {0.125, 0.1875, 0.25, 0.3125, 0.375, 0.4375, 0.5, 0.625, 0.75, 0.875, 1.0, 1.125, 1.25,
                1.375, 1.4375, 1.5, 2.0};
        int seeds = 2048;
        int grid = 32;
        java.util.Random windows = new java.util.Random(0x0153E);
        report.append("mean-spread calibration (std across ").append(seeds)
                .append(" seeds of window/world mean, ").append(grid).append("x").append(grid).append(" grid):\n");
        for (double fraction : fractions) {
            double scale = fraction / WORLD_BLOCKS;
            double[] vanillaMeans = new double[seeds];
            double[] foldMeans = new double[seeds];
            double[] anchors = new double[seeds];
            for (int s = 0; s < seeds; s++) {
                Octave octave = Octave.of(mix(0xCA11B7A7EL + s * 7919L));
                double windowX = windows.nextDouble() * 1.0E6;
                double windowZ = windows.nextDouble() * 1.0E6;
                double vanillaSum = 0.0;
                double foldSum = 0.0;
                for (int i = 0; i < grid; i++) {
                    double x = i * (WORLD_BLOCKS / (double) grid);
                    for (int j = 0; j < grid; j++) {
                        double z = j * (WORLD_BLOCKS / (double) grid);
                        vanillaSum += octave.vanilla().noise(PerlinNoise.wrap((windowX + x) * scale), 0.0,
                                PerlinNoise.wrap((windowZ + z) * scale), 0.0, 0.0);
                        foldSum += sample(octave.permutations(), octave.xo(), octave.yo(),
                                octave.zo(), WORLD, scale, x - 256.0, 0.0, z - 256.0, 0.0, 0.0, -1.0);
                    }
                }
                vanillaMeans[s] = vanillaSum / (grid * grid);
                foldMeans[s] = foldSum / (grid * grid);
                anchors[s] = sample(octave.permutations(), octave.xo(), octave.yo(),
                        octave.zo(), WORLD, scale, 0.0, 0.0, 0.0, 0.0, 0.0, -1.0);
            }
            report.append(String.format(
                    "  f=%.3f cells/lap: vanilla mean-spread %.4f, fold mean-spread %.4f, anchor spread %.4f%n",
                    fraction, Math.sqrt(variance(vanillaMeans)), Math.sqrt(variance(foldMeans)),
                    Math.sqrt(variance(anchors))));
        }
        report.append('\n');
    }

    // Vanilla peaks-and-valleys fold of the ridges field (TerrainProvider.peaksAndValleys): pv = +1 at |r| = 2/3 —
    // the zone where the jaggedness spline injects the 16-octave jagged noise into the terrain.
    private long countPeakZone(double[] ridgeValues) {
        long count = 0;
        for (double ridge : ridgeValues) {
            double pv = -(Math.abs(Math.abs(ridge) - 0.6666666666666666) - 0.3333333333333333) * 3.0;
            if (pv > 0.7) {
                count++;
            }
        }
        return count;
    }

    private double mean(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private double maxAbsDeviation(double[] values) {
        double mean = 0.0;
        for (double value : values) {
            mean += value;
        }
        mean /= values.length;
        double max = 0.0;
        for (double value : values) {
            max = Math.max(max, Math.abs(value - mean));
        }
        return max;
    }

    private String quantileLine(String label, double[] extremes) {
        double[] sorted = extremes.clone();
        Arrays.sort(sorted);
        double mean = 0.0;
        for (double extreme : sorted) {
            mean += extreme;
        }
        mean /= sorted.length;
        return String.format("  %-12s mean=%.4f p50=%.4f p90=%.4f p99=%.4f max=%.4f%n",
                label, mean, sorted[sorted.length / 2], sorted[(int) (sorted.length * 0.9)],
                sorted[(int) (sorted.length * 0.99)], sorted[sorted.length - 1]);
    }

    private double variance(double[] values) {
        double mean = 0.0;
        for (double value : values) {
            mean += value;
        }
        mean /= values.length;
        double varSum = 0.0;
        for (double value : values) {
            varSum += (value - mean) * (value - mean);
        }
        return varSum / values.length;
    }

    private void measureField(StringBuilder report, long worldSeed, int salt, NoiseParams params,
            LayeredNoise offset) {
        LayeredNoise field = new LayeredNoise(worldSeed, salt, params);

        double[][] vanWarp = new double[GRID][GRID];
        double[][] vanFlat = new double[GRID][GRID];
        double[][] modCur = new double[GRID][GRID];
        double[][] modWarp = new double[GRID][GRID];
        double[][] modFix = new double[GRID][GRID];

        for (int i = 0; i < GRID; i++) {
            double x = MIN_BLOCK + i * QUART;
            for (int j = 0; j < GRID; j++) {
                double z = MIN_BLOCK + j * QUART;

                // Vanilla warp: ShiftA feeds (x, 0, z), ShiftB feeds (z, x, 0) — world X rides the noise's Y axis.
                double shiftXVan = offset.vanillaValue(x * XZ_SCALE, z * XZ_SCALE) * 4.0;
                double shiftZVan = shiftBVanilla(offset, x, z) * 4.0;
                vanWarp[i][j] = field.vanillaValue(x * XZ_SCALE + shiftXVan, z * XZ_SCALE + shiftZVan);
                vanFlat[i][j] = field.vanillaValue(x * XZ_SCALE, z * XZ_SCALE);
                modCur[i][j] = field.wrappedValue(x, z, XZ_SCALE);

                // Periodic warp prototype: offsets sampled periodically, then applied pre-scale (x + s/scale keeps
                // closure because the shift itself is periodic). ShiftB's Y-fed world X is folded so the lap closes.
                double shiftXPer = offset.wrappedValue(x, z, XZ_SCALE) * 4.0;
                double shiftZPer = shiftBPeriodic(offset, x, z) * 4.0;
                modWarp[i][j] = field.wrappedValue(x + shiftXPer / XZ_SCALE, z + shiftZPer / XZ_SCALE, XZ_SCALE);
                modFix[i][j] = field.correctedValue(x, z, XZ_SCALE);

                assertTrue(Double.isFinite(vanWarp[i][j]) && Double.isFinite(modCur[i][j])
                        && Double.isFinite(modWarp[i][j]));
            }
        }

        report.append(' ').append(params.name()).append(" (valueFactor ")
                .append(String.format("%.4f", params.valueFactor())).append("):\n");
        Stats envelope = Stats.of(vanWarp);
        Stats fixStats = Stats.of(modFix);
        report.append(envelope.line("van_warp")).append('\n');
        report.append(Stats.of(vanFlat).line("van_flat")).append('\n');
        report.append(Stats.of(modCur).line("mod_cur")).append('\n');
        report.append(Stats.of(modWarp).line("mod_warp")).append('\n');
        report.append(fixStats.line("mod_fix")).append('\n');

        double stdRatio = fixStats.std() / envelope.std();
        this.pooledVanillaVariance += envelope.std() * envelope.std();
        this.pooledUncorrectedVariance += Stats.of(modCur).std() * Stats.of(modCur).std();
        this.pooledCorrectedVariance += fixStats.std() * fixStats.std();
        report.append(String.format("  outside van_warp [min,max]: mod_cur %.2f%%, mod_warp %.2f%%, mod_fix %.2f%%; "
                        + "mod_fix/van_warp std ratio %.3f%n%n",
                shareOutside(modCur, envelope), shareOutside(modWarp, envelope), shareOutside(modFix, envelope),
                stdRatio));
    }

    // Vanilla ShiftB: offsetNoise.getValue(z·0.25, x·0.25, 0) — the second argument is the noise's vertical axis.
    // The replica ignores Y (worldgen samples these fields at y=0 anyway), so vanilla-side we fold the pair into the
    // horizontal plane the same way both sides: (z, x) as (X, Z) of the lattice. This keeps van/mod comparable even
    // though it is not bit-identical to vanilla's Y-fed variant; the distribution is what is being measured.
    private double shiftBVanilla(LayeredNoise offset, double x, double z) {
        return offset.vanillaValue(z * XZ_SCALE, x * XZ_SCALE);
    }

    private double shiftBPeriodic(LayeredNoise offset, double x, double z) {
        return offset.wrappedValue(z, x, XZ_SCALE);
    }

    private double shareOutside(double[][] field, Stats envelope) {
        int outside = 0;
        int total = 0;
        for (double[] row : field) {
            for (double value : row) {
                if (value < envelope.min() || value > envelope.max()) {
                    outside++;
                }
                total++;
            }
        }
        return 100.0 * outside / total;
    }
}
