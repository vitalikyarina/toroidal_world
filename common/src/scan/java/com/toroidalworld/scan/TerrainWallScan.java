package com.toroidalworld.scan;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.engine.noise.ClimateScanFixture;
import com.toroidalworld.engine.noise.ClimateScanFixture.WorldType;
import com.toroidalworld.engine.noise.GenerationTransformerContext;
import com.toroidalworld.engine.noise.OctaveVarianceCorrection;
import com.toroidalworld.shape.WorldLoopPresets;
import com.toroidalworld.shape.torus.ClimateCompression;
import com.toroidalworld.shape.torus.ClimateFields;
import static com.toroidalworld.engine.noise.ClimateScanFixture.SEED_BASE;
import static com.toroidalworld.engine.noise.ClimateScanFixture.TYPES;
import static com.toroidalworld.engine.noise.ClimateScanFixture.noiseParameters;
import static com.toroidalworld.engine.noise.ClimateScanFixture.randomState;
import static com.toroidalworld.engine.noise.ClimateScanFixture.settingsOf;
import static com.toroidalworld.engine.noise.ClimateScanFixture.torusOfWidth;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class TerrainWallScan {
    private static final long WALL_SEED = -4241666765127210365L;

    private static final int WALL_SEEDS = 10;

    private static final int WIDTH_BLOCKS = 512;

    private static final int CELL_WIDTH = 4;

    private static final int CELL_HEIGHT = 8;

    private static final int LEVELS_BELOW_SEA = 1;

    private static final int LEVELS_ABOVE_SEA = 4;

    private static final double BLADE_BLOCKS = 1.5;

    private static final int RIDGE_CORNERS = 3;

    private static final double WIDE = CELL_WIDTH * 2.0;

    private static final int WORST_SITES = 12;

    private static final int NEEDLE_WINDOW_BLOCKS = 48;

    private static final String SCAN = "terrain-wall";

    private static final int NEEDLE_SEEDS = 10;

    private static final long NEEDLE_SEED_STEP = 0x9E3779B97F4A7C15L;

    private static final int NEEDLE_DROP_BLOCKS = 16;

    private static final int COARSE_STEP_BLOCKS = 8;

    private static final int NO_COLUMN = Integer.MIN_VALUE;

    private static final int[][] NEEDLE_NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private static final String NEEDLE_KIND = "needle";

    private static final String PIT_KIND = "pit";

    private static final Path REPORT = ScanReports.DIRECTORY.resolve("terrain-wall-scan.txt");

    private static final Path OCTAVE_REPORT = ScanReports.DIRECTORY.resolve("terrain-wall-octaves.txt");

    private static final Path NEEDLE_REPORT = ScanReports.DIRECTORY.resolve("terrain-needle-scan.txt");

    private static final double CLIMATE_XZ_SCALE = 0.25;

    private static final double HORIZONTAL_SHARE = 0.0;

    private static final List<ResourceKey<NormalNoise.NoiseParameters>> TERRAIN_FIELDS = List.of(
            Noises.CONTINENTALNESS, Noises.EROSION, Noises.RIDGE, Noises.TEMPERATURE, Noises.VEGETATION);

    private record Site(int blockX, int blockY, int blockZ, double thickness, double density,
            double continents, double erosion, double ridges, double depth) {
    }

    private record Field(String name, double mean, double spread, double min, double max, double gradientMean,
            double gradientMax) {
    }

    private record Pass(String name, List<Site> blades, int[] bladesPerLevel, List<Field> fields, double landShare) {
    }

    private record Needles(int needles, int worstDrop, int pits) {
        Needles plus(Needles other) {
            return new Needles(this.needles + other.needles, Math.max(this.worstDrop, other.worstDrop),
                    this.pits + other.pits);
        }
    }

    private record NeedleRow(int seedsWithNeedle, int needles, int worstDrop, int pits, Jumps jumps) {
    }

    private record Spike(String lap, long seed, int blockX, int blockY, int blockZ, String kind, int drop,
            double continents, double erosion, double ridges, double depth) {
    }

    private record Jumps(int p50, int p90, int p99, int p999, int max) {
        static Jumps of(IntArrayList steps) {
            int[] sorted = steps.toIntArray();
            Arrays.sort(sorted);
            return new Jumps(at(sorted, 0.50), at(sorted, 0.90), at(sorted, 0.99), at(sorted, 0.999),
                    sorted[sorted.length - 1]);
        }

        private static int at(int[] sorted, double fraction) {
            return sorted[Math.min(sorted.length - 1, (int) (sorted.length * fraction))];
        }
    }

    @BeforeAll
    static void bootstrapVanilla() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void locatesTheThinTerrainWallsOfACompactedLap() {
        WorldType type = TYPES.getFirst();
        List<Pass> torusPasses = new ArrayList<>();
        List<Pass> controlPasses = new ArrayList<>();

        for (int s = 0; s < WALL_SEEDS; s++) {
            long seed = WALL_SEED + s * NEEDLE_SEED_STEP;
            torusPasses.add(pass("torus " + WIDTH_BLOCKS + " blocks", type, torusOfWidth(WIDTH_BLOCKS), seed));
            controlPasses.add(pass("control (unbounded vanilla)", type, WorldFolds.NOOP, seed));
            ScanReports.note(SCAN, "wall", "type=" + type.name() + " width=" + WIDTH_BLOCKS + " seed=" + seed
                    + " blades=" + torusPasses.getLast().blades().size());
        }

        Pass folded = torusPasses.getFirst();
        Pass control = controlPasses.getFirst();

        StringBuilder report = new StringBuilder();
        report.append("Terrain wall scan - final density on the generator's own cell lattice, over ")
                .append(WALL_SEEDS).append(" seeds from ").append(WALL_SEED).append(", world type ")
                .append(type.name()).append(".")
                .append(System.lineSeparator())
                .append("The generator interpolates final density over ").append(CELL_WIDTH).append("x")
                .append(CELL_HEIGHT).append("x").append(CELL_WIDTH)
                .append("-block cells, so a corner grid of that step carries the whole shape of the terrain.")
                .append(System.lineSeparator())
                .append("thickness = width in blocks of the solid band around a corner, from the linear crossing")
                .append(" to its two neighbours along one axis; a blade is under ")
                .append(String.format(Locale.ROOT, "%.1f", BLADE_BLOCKS))
                .append(" blocks across one axis while at least ").append(RIDGE_CORNERS)
                .append(" corners stay solid along the other.").append(System.lineSeparator())
                .append("Levels: ").append(LEVELS_BELOW_SEA).append(" below and ").append(LEVELS_ABOVE_SEA)
                .append(" above sea level, one per cell height.").append(System.lineSeparator())
                .append("Every field row is read at sea level over the whole lap; grad = absolute change between")
                .append(" corners ").append(CELL_WIDTH).append(" blocks apart.").append(System.lineSeparator())
                .append("per level = blades found at each level, lowest first.").append(System.lineSeparator())
                .append("The control binds WorldFolds.NOOP, so it reads the same window of an unbounded vanilla")
                .append(" world - the same seed, the same coordinates, no fold.").append(System.lineSeparator())
                .append("Jaggedness is not a member of NoiseRouter and is not reported.")
                .append(System.lineSeparator())
                .append("Criterion: the per-site-count clause. The table below is the sampled reading;")
                .append(" the detailed block under it is the first seed alone, kept for comparison with")
                .append(" earlier reports. No blade site is a closing condition; the only assertion is a")
                .append(" blindness guard on an all-water or all-land control window, run on every seed.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        report.append(String.format(Locale.ROOT, "  %-22s %13s %11s %15s %13s%n",
                "seed", "torus blades", "torus land", "control blades", "control land"));

        for (int s = 0; s < WALL_SEEDS; s++) {
            report.append(String.format(Locale.ROOT, "  %-22d %13d %11.3f %15d %13.3f%n",
                    WALL_SEED + s * NEEDLE_SEED_STEP, torusPasses.get(s).blades().size(),
                    torusPasses.get(s).landShare(), controlPasses.get(s).blades().size(),
                    controlPasses.get(s).landShare()));
        }

        report.append(System.lineSeparator());
        for (Pass pass : List.of(folded, control)) {
            appendPass(report, pass);
        }

        List<String> blind = new ArrayList<>();

        for (int s = 0; s < WALL_SEEDS; s++) {
            double share = controlPasses.get(s).landShare();
            if (share <= 0.0 || share >= 1.0) {
                blind.add(Long.toString(WALL_SEED + s * NEEDLE_SEED_STEP));
            }
        }

        report.append("Blind control windows - all water or all land, so that seed measures nothing: ")
                .append(blind.isEmpty() ? "none" : String.join(", ", blind))
                .append(System.lineSeparator())
                .append("A seed's land share is its own luck, from 0.00 to 0.85, so one blind window is a")
                .append(" reading and not a fault; the guard closes only when every seed is blind.")
                .append(System.lineSeparator());

        ScanReports.write(REPORT, ScanReports.population(WALL_SEEDS, WALL_SEED, NEEDLE_SEED_STEP,
                "the whole lattice of a lap per seed, so ten at one width is what the set affords"),
                report.toString());

        assertTrue(blind.size() < WALL_SEEDS,
                "every control window is all water or all land, so the scan measures nothing");
    }

    @Test
    void readsWhatTheFoldAppliesToEachOctaveOfEveryClimateField() {
        WorldFold fold = torusOfWidth(WIDTH_BLOCKS);
        WrapDomain xDomain = fold.blockDomain(Direction.Axis.X);
        WrapDomain zDomain = fold.blockDomain(Direction.Axis.Z);

        StringBuilder report = new StringBuilder();
        report.append("Octave table - what the fold applies to every octave of the router's climate fields on a ")
                .append(WIDTH_BLOCKS).append("-block torus, read off the code that applies it.")
                .append(System.lineSeparator())
                .append("Every field reaches the sampler as a shifted_noise at xz scale ").append(CLIMATE_XZ_SCALE)
                .append(" with no vertical share, so the octave scale is ").append(CLIMATE_XZ_SCALE)
                .append(" x compression x 2^firstOctave x 2^octave.").append(System.lineSeparator())
                .append("compression = ClimateCompression.factor under Compact biomes Auto.")
                .append(System.lineSeparator())
                .append("cells = cells the octave carries over one lap; period = the lattice it closes on, marked")
                .append(" floored where the natural one falls under 2 and LapFloor hands out its own.")
                .append(System.lineSeparator())
                .append("damp = OctaveVarianceCorrection.factor, gain = its anchor gain; an octave the floor")
                .append(" never caught takes damp 1 and gain 0.")
                .append(System.lineSeparator())
                .append("Criterion: none. This table reads what the fold applies to each octave and measures")
                .append(" no terrain, so it gates nothing.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        for (ResourceKey<NormalNoise.NoiseParameters> key : TERRAIN_FIELDS) {
            appendField(report, key, xDomain, zDomain, fold);
        }

        ScanReports.write(OCTAVE_REPORT, ScanReports.noPopulation(
                "the router's own amplitude weights, read without a world"), report.toString());
    }

    @Test
    void countsTheNeedlesEveryPresetLeavesOnItsLap() {
        WorldType type = TYPES.getFirst();
        NoiseSettings noiseSettings = settingsOf(type).noiseSettings();

        StringBuilder report = new StringBuilder();
        report.append("Needle scan - the highest solid block of every column, walked down the real final density,")
                .append(" no chunk generation.").append(System.lineSeparator())
                .append("A needle is a column standing at least ").append(NEEDLE_DROP_BLOCKS)
                .append(" blocks above all four of its neighbours; the blade metric of the wall scan reads the")
                .append(" generator's ").append(CELL_WIDTH).append("-block corner lattice and cannot see one.")
                .append(System.lineSeparator())
                .append("Per preset: ").append(NEEDLE_SEEDS).append(" seeds, one ").append(NEEDLE_WINDOW_BLOCKS)
                .append("x").append(NEEDLE_WINDOW_BLOCKS).append("-block window in each quadrant of the lap.")
                .append(System.lineSeparator())
                .append("The column is walked from the top of the dimension down in ").append(COARSE_STEP_BLOCKS)
                .append("-block steps and refined to the block, so a solid layer thinner than one step is")
                .append(" reported only where it is the highest one.").append(System.lineSeparator())
                .append("pits = the same test downward: columns sitting that far below all four neighbours.")
                .append(" Reported, not gated - ordinary seabed relief clears the same threshold.")
                .append(System.lineSeparator())
                .append("The control binds WorldFolds.NOOP, so it reads the same windows of an unbounded vanilla")
                .append(" world - the same seed, the same coordinates, no fold.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        report.append("The gate is the distribution: every adjacent-column height step over the windows, torus")
                .append(" against control. No percentile up to p999 may run more than one needle (")
                .append(NEEDLE_DROP_BLOCKS).append(" blocks) past the control's; max is one column and swings")
                .append(" both ways, so it is printed and not gated. The needle and pit counts under it are")
                .append(" outliers of the same numbers, reported so a row can be followed to a place.")
                .append(System.lineSeparator())
                .append("Criterion: the distribution clause on the percentiles, which are a distribution gate")
                .append(" against the unbounded control, and the per-site-count clause on the needle and pit")
                .append(" counts, which are readings.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        report.append(String.format(Locale.ROOT, "    %-8s %-14s %29s %29s%n", "", "", "torus", "control"));
        report.append(String.format(Locale.ROOT, "    %-8s %-14s %5s %5s %5s %5s %5s %5s %5s %5s %5s %5s%n",
                "preset", "blocks", "p50", "p90", "p99", "p999", "max", "p50", "p90", "p99", "p999", "max"));

        List<Spike> sites = new ArrayList<>();
        List<NeedleRow> torusRows = new ArrayList<>();
        List<NeedleRow> controlRows = new ArrayList<>();
        List<String> widened = new ArrayList<>();

        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            int widthBlocks = preset.blockWidth();
            NeedleRow torus = needleRow(type, noiseSettings, widthBlocks, true, preset.id(), sites);
            NeedleRow control = needleRow(type, noiseSettings, widthBlocks, false, preset.id(), sites);
            torusRows.add(torus);
            controlRows.add(control);
            widened.addAll(overrun(preset.id(), torus.jumps(), control.jumps()));

            report.append(String.format(Locale.ROOT, "    %-8s %-14s %5d %5d %5d %5d %5d %5d %5d %5d %5d %5d%n",
                    preset.id(), widthBlocks + " blocks",
                    torus.jumps().p50(), torus.jumps().p90(), torus.jumps().p99(), torus.jumps().p999(),
                    torus.jumps().max(),
                    control.jumps().p50(), control.jumps().p90(), control.jumps().p99(),
                    control.jumps().p999(), control.jumps().max()));
        }

        report.append(System.lineSeparator())
                .append("Outliers of the same windows, ").append(NEEDLE_SEEDS).append(" seeds per preset:")
                .append(System.lineSeparator());
        report.append(String.format(Locale.ROOT, "    %-8s %-14s %23s %23s%n", "", "", "torus", "control"));
        report.append(String.format(Locale.ROOT, "    %-8s %-14s %5s %7s %5s %6s %5s %7s %5s %6s%n",
                "preset", "blocks", "seeds", "needles", "worst", "pits", "seeds", "needles", "worst", "pits"));

        int row = 0;
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            NeedleRow torus = torusRows.get(row);
            NeedleRow control = controlRows.get(row++);
            report.append(String.format(Locale.ROOT, "    %-8s %-14s %5d %7d %5d %6d %5d %7d %5d %6d%n",
                    preset.id(), preset.blockWidth() + " blocks",
                    torus.seedsWithNeedle(), torus.needles(), torus.worstDrop(), torus.pits(),
                    control.seedsWithNeedle(), control.needles(), control.worstDrop(), control.pits()));
        }

        report.append(System.lineSeparator())
                .append("seeds = seeds carrying at least one needle; needles = the count over every window;")
                .append(" worst = the largest drop to a neighbour.")
                .append(System.lineSeparator());

        appendSites(report, sites);
        ScanReports.write(NEEDLE_REPORT, ScanReports.population(NEEDLE_SEEDS, SEED_BASE, NEEDLE_SEED_STEP,
                "seconds per seed, so ten is the ceiling the set affords over ten preset rows"),
                report.toString());

        assertTrue(widened.isEmpty(), "a folded lap steps higher between neighbours than its control: " + widened);
    }

    private static List<String> overrun(String presetId, Jumps torus, Jumps control) {
        List<String> over = new ArrayList<>();
        int[][] pairs = {
                {torus.p50(), control.p50()}, {torus.p90(), control.p90()},
                {torus.p99(), control.p99()}, {torus.p999(), control.p999()}};
        String[] names = {"p50", "p90", "p99", "p999"};

        for (int i = 0; i < pairs.length; i++) {
            if (pairs[i][0] - pairs[i][1] > NEEDLE_DROP_BLOCKS) {
                over.add(presetId + " " + names[i] + ": torus " + pairs[i][0] + " blocks, control "
                        + pairs[i][1]);
            }
        }

        return over;
    }

    private static void appendSites(StringBuilder report, List<Spike> sites) {
        appendSites(report, sites, NEEDLE_KIND, "Needles, largest drop first:");
        appendSites(report, sites, PIT_KIND, "Pits, largest drop first:");
    }

    private static void appendSites(StringBuilder report, List<Spike> sites, String kind, String title) {
        List<Spike> ofKind = sites.stream()
                .filter(spike -> spike.kind().equals(kind))
                .sorted(Comparator.comparingInt(Spike::drop).reversed())
                .limit(WORST_SITES)
                .toList();
        if (ofKind.isEmpty()) {
            return;
        }

        report.append(System.lineSeparator()).append(title).append(System.lineSeparator());
        report.append(String.format(Locale.ROOT, "    %-16s %-18s %5s %10s %9s %9s %9s %22s%n",
                "lap", "x y z", "drop", "continents", "erosion", "ridges", "depth", "seed"));

        for (Spike spike : ofKind) {
            report.append(String.format(Locale.ROOT, "    %-16s %-18s %5d %10.4f %9.4f %9.4f %9.4f %22d%n",
                    spike.lap(), spike.blockX() + " " + spike.blockY() + " " + spike.blockZ(),
                    spike.drop(), spike.continents(), spike.erosion(), spike.ridges(), spike.depth(),
                    spike.seed()));
        }
    }

    private static NeedleRow needleRow(WorldType type, NoiseSettings noiseSettings, int widthBlocks,
            boolean folded, String presetId, List<Spike> sites) {
        int seedsWithNeedle = 0;
        int needles = 0;
        int worstDrop = 0;
        int sunkenColumns = 0;
        IntArrayList steps = new IntArrayList();

        for (int s = 0; s < NEEDLE_SEEDS; s++) {
            long seed = SEED_BASE + s * NEEDLE_SEED_STEP;
            WorldFold fold = folded ? torusOfWidth(widthBlocks) : WorldFolds.NOOP;
            Needles found = quadrants(type, noiseSettings, fold, widthBlocks, seed,
                    presetId + (folded ? " torus" : " control"), sites, steps);
            if (found.needles() > 0) {
                seedsWithNeedle++;
            }

            needles += found.needles();
            worstDrop = Math.max(worstDrop, found.worstDrop());
            sunkenColumns += found.pits();
            ScanReports.note(SCAN, "needle", "preset=" + presetId + " folded=" + folded + " width=" + widthBlocks
                    + " seed=" + seed + " needles=" + found.needles());
        }

        return new NeedleRow(seedsWithNeedle, needles, worstDrop, sunkenColumns, Jumps.of(steps));
    }

    private static Needles quadrants(WorldType type, NoiseSettings noiseSettings, WorldFold fold,
            int widthBlocks, long seed, String lap, List<Spike> sites, IntArrayList steps) {
        NoiseRouter router = randomState(type, fold, seed).router();
        Needles[] total = {new Needles(0, 0, 0)};

        GenerationTransformerContext.runWithTransformer(fold, () -> {
            for (int quadrantX = 0; quadrantX < 2; quadrantX++) {
                for (int quadrantZ = 0; quadrantZ < 2; quadrantZ++) {
                    total[0] = total[0].plus(window(router, noiseSettings,
                            quadrantX * widthBlocks / 2, quadrantZ * widthBlocks / 2, lap, seed, sites, steps));
                }
            }
        });

        return total[0];
    }

    private static Needles window(NoiseRouter router, NoiseSettings noiseSettings, int originX, int originZ,
            String lap, long seed, List<Spike> sites, IntArrayList steps) {
        DensityFunction density = router.finalDensity();
        int span = NEEDLE_WINDOW_BLOCKS + 2;
        int[] heights = new int[span * span];

        for (int dx = 0; dx < span; dx++) {
            for (int dz = 0; dz < span; dz++) {
                heights[dx * span + dz] = surfaceHeight(density, noiseSettings, originX + dx - 1, originZ + dz - 1);
            }
        }

        int needles = 0;
        int worstDrop = 0;
        int pits = 0;

        for (int dx = 1; dx <= NEEDLE_WINDOW_BLOCKS; dx++) {
            for (int dz = 1; dz <= NEEDLE_WINDOW_BLOCKS; dz++) {
                int here = height(heights[dx * span + dz], noiseSettings);
                steps.add(Math.abs(here - height(heights[(dx + 1) * span + dz], noiseSettings)));
                steps.add(Math.abs(here - height(heights[dx * span + dz + 1], noiseSettings)));
                int rise = Integer.MAX_VALUE;
                int fall = Integer.MAX_VALUE;

                for (int[] step : NEEDLE_NEIGHBOURS) {
                    int neighbour = height(heights[(dx + step[0]) * span + dz + step[1]], noiseSettings);
                    rise = Math.min(rise, here - neighbour);
                    fall = Math.min(fall, neighbour - here);
                }

                if (rise >= NEEDLE_DROP_BLOCKS) {
                    needles++;
                    worstDrop = Math.max(worstDrop, rise);
                    sites.add(spike(router, lap, seed, originX + dx - 1, here, originZ + dz - 1,
                            NEEDLE_KIND, rise));
                }

                if (fall >= NEEDLE_DROP_BLOCKS) {
                    pits++;
                    sites.add(spike(router, lap, seed, originX + dx - 1, here, originZ + dz - 1, PIT_KIND, fall));
                }
            }
        }

        return new Needles(needles, worstDrop, pits);
    }

    private static Spike spike(NoiseRouter router, String lap, long seed, int blockX, int blockY, int blockZ,
            String kind, int drop) {
        DensityFunction.SinglePointContext point = new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
        return new Spike(lap, seed, blockX, blockY, blockZ, kind, drop,
                router.continents().compute(point), router.erosion().compute(point),
                router.ridges().compute(point), router.depth().compute(point));
    }

    private static int height(int found, NoiseSettings noiseSettings) {
        return found == NO_COLUMN ? noiseSettings.minY() : found;
    }

    private static int surfaceHeight(DensityFunction density, NoiseSettings noiseSettings, int blockX, int blockZ) {
        int bottom = noiseSettings.minY();
        int top = bottom + noiseSettings.height() - 1;

        for (int y = top; y >= bottom; y -= COARSE_STEP_BLOCKS) {
            if (density.compute(new DensityFunction.SinglePointContext(blockX, y, blockZ)) <= 0.0) {
                continue;
            }

            for (int fine = Math.min(top, y + COARSE_STEP_BLOCKS - 1); fine > y; fine--) {
                if (density.compute(new DensityFunction.SinglePointContext(blockX, fine, blockZ)) > 0.0) {
                    return fine;
                }
            }

            return y;
        }

        return NO_COLUMN;
    }

    private static void appendField(StringBuilder report, ResourceKey<NormalNoise.NoiseParameters> key,
            WrapDomain xDomain, WrapDomain zDomain, WorldFold fold) {
        NormalNoise.NoiseParameters parameters = noiseParameters(key);
        DoubleList amplitudes = parameters.amplitudes();
        double lowestFreqInputFactor = Math.pow(2.0, parameters.firstOctave());
        double compression = ClimateCompression.factor(fold, ClimateFields.isClimate(key), amplitudes,
                lowestFreqInputFactor, CLIMATE_XZ_SCALE, HORIZONTAL_SHARE);

        report.append("  ").append(key.identifier().getPath())
                .append(String.format(Locale.ROOT, ", first octave %d, compression %.3f%n",
                        parameters.firstOctave(), compression));
        report.append(String.format(Locale.ROOT, "    %-7s %10s %9s %8s %7s %7s%n",
                "octave", "amplitude", "cells", "period", "damp", "gain"));

        for (int i = 0; i < amplitudes.size(); i++) {
            double scale = CLIMATE_XZ_SCALE * compression * lowestFreqInputFactor * Math.pow(2.0, i);
            long natural = Math.round(WIDTH_BLOCKS * scale);
            report.append(String.format(Locale.ROOT, "    %-7d %10.3f %9.4f %8s %7.3f %7.3f%n",
                    i, amplitudes.getDouble(i), WIDTH_BLOCKS * scale,
                    natural < 2L ? natural + " floored" : Long.toString(natural),
                    OctaveVarianceCorrection.factor(xDomain, zDomain, scale, HORIZONTAL_SHARE),
                    OctaveVarianceCorrection.anchorGain(xDomain, zDomain, scale, HORIZONTAL_SHARE)));
        }

        report.append(System.lineSeparator());
    }

    private static void appendPass(StringBuilder report, Pass pass) {
        report.append("  ").append(pass.name()).append(System.lineSeparator());
        report.append(String.format(Locale.ROOT, "    land share %.3f, blades %d, per level %s%n",
                pass.landShare(), pass.blades().size(), levels(pass.bladesPerLevel())));
        report.append(String.format(Locale.ROOT, "    %-16s %9s %9s %9s %9s %10s %10s%n",
                "field", "mean", "spread", "min", "max", "grad mean", "grad max"));

        for (Field field : pass.fields()) {
            report.append(String.format(Locale.ROOT, "    %-16s %9.4f %9.4f %9.4f %9.4f %10.4f %10.4f%n",
                    field.name(), field.mean(), field.spread(), field.min(), field.max(),
                    field.gradientMean(), field.gradientMax()));
        }

        if (!pass.blades().isEmpty()) {
            report.append(String.format(Locale.ROOT, "    %-22s %9s %9s %10s %9s %9s %9s%n",
                    "thinnest blades", "thick", "density", "continents", "erosion", "ridges", "depth"));

            for (Site site : pass.blades().stream()
                    .sorted(Comparator.comparingDouble(Site::thickness))
                    .limit(WORST_SITES)
                    .toList()) {
                report.append(String.format(Locale.ROOT, "    %-22s %9.2f %9.4f %10.4f %9.4f %9.4f %9.4f%n",
                        site.blockX() + " " + site.blockY() + " " + site.blockZ(),
                        site.thickness(), site.density(), site.continents(), site.erosion(), site.ridges(),
                        site.depth()));
            }
        }

        report.append(System.lineSeparator());
    }

    private static Pass pass(String name, WorldType type, WorldFold fold, long seed) {
        RandomState randomState = randomState(type, fold, seed);
        NoiseRouter router = randomState.router();
        int seaLevel = settingsOf(type).seaLevel();
        int corners = WIDTH_BLOCKS / CELL_WIDTH;
        List<Site> blades = new ArrayList<>();
        List<Field> fields = new ArrayList<>();
        double[] land = new double[1];
        int[] perLevel = new int[LEVELS_BELOW_SEA + LEVELS_ABOVE_SEA + 1];

        GenerationTransformerContext.runWithTransformer(fold, () -> {
            double[] sea = grid(router.finalDensity(), corners, seaLevel);
            fields.add(field("final density", sea, corners));
            fields.add(field("continents", grid(router.continents(), corners, seaLevel), corners));
            fields.add(field("erosion", grid(router.erosion(), corners, seaLevel), corners));
            fields.add(field("ridges", grid(router.ridges(), corners, seaLevel), corners));
            fields.add(field("depth", grid(router.depth(), corners, seaLevel), corners));
            land[0] = solidShare(sea);

            for (int level = -LEVELS_BELOW_SEA; level <= LEVELS_ABOVE_SEA; level++) {
                int blockY = seaLevel + level * CELL_HEIGHT;
                double[] density = level == 0 ? sea : grid(router.finalDensity(), corners, blockY);
                int before = blades.size();
                collectBlades(blades, router, density, corners, blockY);
                perLevel[level + LEVELS_BELOW_SEA] = blades.size() - before;
            }
        });

        return new Pass(name, List.copyOf(blades), perLevel, List.copyOf(fields), land[0]);
    }

    private static String levels(int[] perLevel) {
        StringBuilder counts = new StringBuilder();

        for (int index = 0; index < perLevel.length; index++) {
            counts.append(index == 0 ? "" : "/").append(perLevel[index]);
        }

        return counts.toString();
    }

    private static void collectBlades(List<Site> blades, NoiseRouter router, double[] density, int corners,
            int blockY) {
        for (int ix = 0; ix < corners; ix++) {
            for (int iz = 0; iz < corners; iz++) {
                if (at(density, corners, ix, iz) <= 0.0) {
                    continue;
                }

                double acrossX = thickness(density, corners, ix, iz, true);
                double acrossZ = thickness(density, corners, ix, iz, false);
                boolean wall = acrossX < BLADE_BLOCKS && solidRun(density, corners, ix, iz, false) >= RIDGE_CORNERS
                        || acrossZ < BLADE_BLOCKS && solidRun(density, corners, ix, iz, true) >= RIDGE_CORNERS;
                if (!wall) {
                    continue;
                }

                int blockX = ix * CELL_WIDTH;
                int blockZ = iz * CELL_WIDTH;
                DensityFunction.SinglePointContext point =
                        new DensityFunction.SinglePointContext(blockX, blockY, blockZ);
                blades.add(new Site(blockX, blockY, blockZ, Math.min(acrossX, acrossZ),
                        at(density, corners, ix, iz),
                        router.continents().compute(point), router.erosion().compute(point),
                        router.ridges().compute(point), router.depth().compute(point)));
            }
        }
    }

    private static double thickness(double[] density, int corners, int ix, int iz, boolean alongX) {
        double here = at(density, corners, ix, iz);
        double before = alongX ? at(density, corners, ix - 1, iz) : at(density, corners, ix, iz - 1);
        double after = alongX ? at(density, corners, ix + 1, iz) : at(density, corners, ix, iz + 1);
        if (before > 0.0 || after > 0.0) {
            return WIDE;
        }

        return CELL_WIDTH * (here / (here - before)) + CELL_WIDTH * (here / (here - after));
    }

    private static int solidRun(double[] density, int corners, int ix, int iz, boolean alongX) {
        int run = 1;

        for (int step = 1; step < corners; step++) {
            double forward = alongX ? at(density, corners, ix + step, iz) : at(density, corners, ix, iz + step);
            if (forward <= 0.0) {
                break;
            }

            run++;
        }

        for (int step = 1; step < corners; step++) {
            double back = alongX ? at(density, corners, ix - step, iz) : at(density, corners, ix, iz - step);
            if (back <= 0.0) {
                break;
            }

            run++;
        }

        return run;
    }

    private static double[] grid(DensityFunction function, int corners, int blockY) {
        double[] values = new double[corners * corners];

        for (int ix = 0; ix < corners; ix++) {
            for (int iz = 0; iz < corners; iz++) {
                values[ix * corners + iz] = function.compute(
                        new DensityFunction.SinglePointContext(ix * CELL_WIDTH, blockY, iz * CELL_WIDTH));
            }
        }

        return values;
    }

    private static Field field(String name, double[] values, int corners) {
        double sum = 0.0;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double gradientSum = 0.0;
        double gradientMax = 0.0;

        for (int ix = 0; ix < corners; ix++) {
            for (int iz = 0; iz < corners; iz++) {
                double here = at(values, corners, ix, iz);
                sum += here;
                min = Math.min(min, here);
                max = Math.max(max, here);

                double alongX = Math.abs(at(values, corners, ix + 1, iz) - here);
                double alongZ = Math.abs(at(values, corners, ix, iz + 1) - here);
                gradientSum += alongX + alongZ;
                gradientMax = Math.max(gradientMax, Math.max(alongX, alongZ));
            }
        }

        double mean = sum / values.length;
        double variance = 0.0;

        for (double value : values) {
            variance += (value - mean) * (value - mean);
        }

        return new Field(name, mean, Math.sqrt(variance / values.length), min, max,
                gradientSum / (values.length * 2), gradientMax);
    }

    private static double solidShare(double[] values) {
        int solid = 0;

        for (double value : values) {
            if (value > 0.0) {
                solid++;
            }
        }

        return solid / (double) values.length;
    }

    private static double at(double[] values, int corners, int ix, int iz) {
        return values[Math.floorMod(ix, corners) * corners + Math.floorMod(iz, corners)];
    }
}
