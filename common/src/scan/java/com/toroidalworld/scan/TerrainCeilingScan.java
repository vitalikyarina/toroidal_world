package com.toroidalworld.scan;

import static com.toroidalworld.noise.ClimateScanFixture.SEED_BASE;
import static com.toroidalworld.noise.ClimateScanFixture.randomState;
import static com.toroidalworld.noise.ClimateScanFixture.settingsOf;
import static com.toroidalworld.noise.ClimateScanFixture.torusOfWidth;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.ClimateScanFixture;
import com.toroidalworld.noise.ClimateScanFixture.WorldType;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.TerrainCeiling;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;

class TerrainCeilingScan {
    private static final List<String> OVERWORLD_TYPES = List.of("default", "large biomes", "amplified");

    private static final int WIDTH_BLOCKS = 512;

    private static final int GRID = 64;

    private static final int SEEDS = 4;

    private static final long SEED_STEP = 0x9E3779B97F4A7C15L;

    private static final int TOP_Y = 319;

    private static final int FLOOR_Y = 32;

    private static final int SEARCH_FLOOR_Y = -64;

    private static final int COARSE_STEP_BLOCKS = 4;

    private static final int RAMP_BLOCKS = 16;

    private static final double FLAT_HEADROOM_BLOCKS = 4.0;

    private static final double OVERSHOOT_SHARE_CEILING = 0.02;

    private static final double JAGGED_DRIFT_BLOCKS = 24.0;

    private static final Path REPORTS =
            Path.of(System.getProperty("toroidal.reports", "build/reports")).resolve("scan");

    private static final Path REPORT = REPORTS.resolve("terrain-ceiling-scan.txt");

    private static final Path ISLAND_REPORT = REPORTS.resolve("terrain-ceiling-island.txt");

    private static final String ISLAND_TYPE = "default";

    private static final long ISLAND_SEED = 9059185195617760112L;

    private static final int ISLAND_WIDTH_BLOCKS = 512;

    private static final int ISLAND_X = 18;

    private static final int ISLAND_Z = -184;

    private static final int ISLAND_WINDOW_BLOCKS = 96;

    private static final int ISLAND_STRIDE_BLOCKS = 2;

    private static final int ISLAND_MIN_CUT_COLUMNS = 16;

    private static final int ISLAND_MIN_CUT_BLOCKS = 30;

    private static final int GRAZING_BLOCKS = 8;

    private record Column(int top, double surface, double ceiling, double densityAboveCeiling) {
        double overshoot() {
            return this.top - this.ceiling;
        }

        double headroom() {
            return this.ceiling - this.surface;
        }
    }

    private record Bucket(String name, int columns, double overshootShare, double overshootP99, double maxOvershoot) {
    }

    @BeforeAll
    static void bootstrap() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void theCeilingClearsConnectedTerrainOnEveryOverworldType() {
        List<String> report = new ArrayList<>();
        report.add("Terrain ceiling — how far the topmost solid block stands above the ceiling");
        report.add("world " + WIDTH_BLOCKS + " blocks, " + GRID + "x" + GRID + " columns one every "
                + (WIDTH_BLOCKS / GRID) + " blocks, " + SEEDS + " seeds");
        report.add("");

        List<Bucket> jaggedBuckets = new ArrayList<>();
        List<Bucket> flatBuckets = new ArrayList<>();

        for (WorldType type : ClimateScanFixture.TYPES) {
            if (!OVERWORLD_TYPES.contains(type.name())) {
                continue;
            }

            boolean ceilinged = TerrainCeiling.ceiling(settingsOf(type)) != null;
            report.add(type.name() + (ceilinged ? "" : " — no ceiling installed"));
            if (!ceilinged) {
                report.add("");
                continue;
            }

            List<Column> columns = sample(type);
            report.add("  columns sampled: " + columns.size());
            Bucket flat = bucket("flat", columns.stream()
                    .filter(column -> column.headroom() <= flatHeadroomCeiling(columns)).toList());
            Bucket jagged = bucket("jagged", columns.stream()
                    .filter(column -> column.headroom() > flatHeadroomCeiling(columns)).toList());
            flatBuckets.add(flat);
            jaggedBuckets.add(jagged);

            report.add("  preliminary surface: " + spread(columns, Column::surface) + " blocks");
            report.add("  topmost solid block: " + spread(columns, column -> column.top()) + " blocks");
            report.add("  topmost solid above the preliminary surface: "
                    + spread(columns, column -> column.top() - column.surface()) + " blocks");
            report.add("  columns whose preliminary surface sits at the search floor (y=" + SEARCH_FLOOR_Y + "): "
                    + columns.stream().filter(column -> column.surface() <= SEARCH_FLOOR_Y).count());
            report.add("  headroom above the preliminary surface: min "
                    + round(columns.stream().mapToDouble(Column::headroom).min().orElse(0.0))
                    + " blocks, max " + round(columns.stream().mapToDouble(Column::headroom).max().orElse(0.0))
                    + " blocks");
            report.add("  vanilla density one ramp above the ceiling: "
                    + spread(columns, Column::densityAboveCeiling) + " (the penalty has to outweigh it)");
            report.add(line(flat));
            report.add(line(jagged));
            report.add("");
        }

        write(report);

        for (Bucket bucket : flatBuckets) {
            assertTrue(bucket.overshootShare() <= OVERSHOOT_SHARE_CEILING,
                    "flat columns standing above the ceiling: " + round(bucket.overshootShare())
                            + " of them, ceiling " + OVERSHOOT_SHARE_CEILING);
        }

        for (int i = 0; i < jaggedBuckets.size(); i++) {
            double drift = jaggedBuckets.get(i).overshootP99() - flatBuckets.get(i).overshootP99();
            assertTrue(drift <= JAGGED_DRIFT_BLOCKS,
                    "the jaggedness lift is the wrong size: jagged columns overshoot the ceiling by "
                            + round(drift) + " blocks more than flat ones at p99, drift ceiling "
                            + JAGGED_DRIFT_BLOCKS + " blocks");
        }
    }

    private static String spread(List<Column> columns, java.util.function.ToDoubleFunction<Column> reading) {
        double[] sorted = columns.stream().mapToDouble(reading).sorted().toArray();
        return "min " + round(sorted[0]) + ", p50 " + round(percentile(sorted, 0.50))
                + ", p99 " + round(percentile(sorted, 0.99)) + ", max " + round(sorted[sorted.length - 1]);
    }

    private static double flatHeadroomCeiling(List<Column> columns) {
        return columns.stream().mapToDouble(Column::headroom).min().orElse(0.0) + FLAT_HEADROOM_BLOCKS;
    }

    private static Bucket bucket(String name, List<Column> columns) {
        if (columns.isEmpty()) {
            return new Bucket(name, 0, 0.0, 0.0, 0.0);
        }

        double[] overshoots = columns.stream().mapToDouble(Column::overshoot).sorted().toArray();
        long above = Arrays.stream(overshoots).filter(overshoot -> overshoot > 0.0).count();
        return new Bucket(name, columns.size(), above / (double) columns.size(),
                percentile(overshoots, 0.99), overshoots[overshoots.length - 1]);
    }

    private static double percentile(double[] sorted, double share) {
        return sorted[Math.min(sorted.length - 1, (int) (sorted.length * share))];
    }

    private static String line(Bucket bucket) {
        return "  " + bucket.name() + ": " + bucket.columns() + " columns, "
                + round(bucket.overshootShare()) + " of them above the ceiling, p99 "
                + round(bucket.overshootP99()) + " blocks, worst " + round(bucket.maxOvershoot()) + " blocks";
    }

    private static List<Column> sample(WorldType type) {
        List<Column> columns = new ArrayList<>();
        WorldFold fold = torusOfWidth(WIDTH_BLOCKS);
        NoiseGeneratorSettings settings = settingsOf(type);
        DensityFunction rawCeiling = TerrainCeiling.ceiling(settings);
        assertTrue(rawCeiling != null, "no jaggedness node in the " + type.name() + " router — nothing to measure");

        NoiseGeneratorSettings probe = withCeilingParked(settings, rawCeiling);
        double step = WIDTH_BLOCKS / (double) GRID;

        for (int s = 0; s < SEEDS; s++) {
            RandomState randomState = randomState(probe, fold, SEED_BASE + s * SEED_STEP);
            NoiseRouter router = randomState.router();
            DensityFunction ceiling = router.barrierNoise();
            DensityFunction density = router.finalDensity();
            DensityFunction surface = router.initialDensityWithoutJaggedness();
            GenerationTransformerContext.runWithTransformer(fold, () -> {
                for (int ix = 0; ix < GRID; ix++) {
                    for (int iz = 0; iz < GRID; iz++) {
                        int blockX = (int) Math.round(ix * step);
                        int blockZ = (int) Math.round(iz * step);
                        int top = topSolid(density, blockX, blockZ, settings.seaLevel());
                        if (top == Integer.MIN_VALUE) {
                            continue;
                        }

                        double ceilingY = at(ceiling, blockX, blockZ);
                        int probeY = (int) Math.min(TOP_Y, Math.round(ceilingY + RAMP_BLOCKS));
                        columns.add(new Column(top, at(surface, blockX, blockZ), ceilingY,
                                density.compute(new DensityFunction.SinglePointContext(blockX, probeY, blockZ))));
                    }
                }
            });
        }

        return columns;
    }

    @SuppressWarnings("deprecation")
    static NoiseGeneratorSettings withCeilingParked(NoiseGeneratorSettings settings, DensityFunction ceiling) {
        NoiseRouter source = settings.noiseRouter();
        NoiseRouter parked = new NoiseRouter(
                ceiling,
                source.fluidLevelFloodednessNoise(),
                source.fluidLevelSpreadNoise(),
                source.lavaNoise(),
                source.temperature(),
                source.vegetation(),
                source.continents(),
                source.erosion(),
                source.depth(),
                source.ridges(),
                source.initialDensityWithoutJaggedness(),
                source.finalDensity(),
                source.veinToggle(),
                source.veinRidged(),
                source.veinGap());
        return new NoiseGeneratorSettings(
                settings.noiseSettings(),
                settings.defaultBlock(),
                settings.defaultFluid(),
                parked,
                settings.surfaceRule(),
                settings.spawnTarget(),
                settings.seaLevel(),
                settings.disableMobGeneration(),
                settings.aquifersEnabled(),
                settings.oreVeinsEnabled(),
                settings.useLegacyRandomSource());
    }

    private static int topSolid(DensityFunction density, int blockX, int blockZ, int seaLevel) {
        int floor = Math.min(FLOOR_Y, seaLevel);
        for (int y = TOP_Y; y >= floor; y -= COARSE_STEP_BLOCKS) {
            if (density.compute(new DensityFunction.SinglePointContext(blockX, y, blockZ)) <= 0.0) {
                continue;
            }

            for (int refined = Math.min(TOP_Y, y + COARSE_STEP_BLOCKS - 1); refined > y; refined--) {
                if (density.compute(new DensityFunction.SinglePointContext(blockX, refined, blockZ)) > 0.0) {
                    return refined;
                }
            }

            return y;
        }

        return Integer.MIN_VALUE;
    }

    private static double at(DensityFunction function, int blockX, int blockZ) {
        return function.compute(new DensityFunction.SinglePointContext(blockX, 0, blockZ));
    }

    private static String round(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static void write(List<String> lines) {
        write(REPORT, lines);
    }

    private static void write(Path report, List<String> lines) {
        try {
            Files.createDirectories(REPORTS);
            Files.write(report, lines);
        } catch (IOException failed) {
            throw new UncheckedIOException(failed);
        }
    }

    @Test
    void theKnownIslandGoesAndNothingBelowTheCeilingMoves() {
        WorldType type = ClimateScanFixture.TYPES.stream()
                .filter(candidate -> candidate.name().equals(ISLAND_TYPE))
                .findFirst()
                .orElseThrow();
        WorldFold fold = torusOfWidth(ISLAND_WIDTH_BLOCKS);
        NoiseGeneratorSettings vanilla = settingsOf(type);
        DensityFunction rawCeiling = TerrainCeiling.ceiling(vanilla);
        assertTrue(rawCeiling != null, "no ceiling for the " + ISLAND_TYPE + " settings");

        RandomState probeState = randomState(withCeilingParked(vanilla, rawCeiling), fold, ISLAND_SEED);
        RandomState cutState = randomState(TerrainCeiling.withCeiling(vanilla), fold, ISLAND_SEED);
        DensityFunction ceiling = probeState.router().barrierNoise();
        DensityFunction vanillaDensity = probeState.router().finalDensity();
        DensityFunction cutDensity = cutState.router().finalDensity();
        int seaLevel = vanilla.seaLevel();

        List<String> report = new ArrayList<>();
        report.add("Known floating island — seed " + ISLAND_SEED + ", " + ISLAND_TYPE + ", lap "
                + ISLAND_WIDTH_BLOCKS + " blocks");
        report.add("window " + ISLAND_WINDOW_BLOCKS + " blocks around x=" + ISLAND_X + " z=" + ISLAND_Z
                + ", one column every " + ISLAND_STRIDE_BLOCKS + " blocks");
        report.add("");

        int[] counts = new int[3];
        List<Double> drops = new ArrayList<>();
        List<Double> aboves = new ArrayList<>();
        List<Double> insides = new ArrayList<>();
        GenerationTransformerContext.runWithTransformer(fold, () -> {
            for (int dx = -ISLAND_WINDOW_BLOCKS / 2; dx <= ISLAND_WINDOW_BLOCKS / 2; dx += ISLAND_STRIDE_BLOCKS) {
                for (int dz = -ISLAND_WINDOW_BLOCKS / 2; dz <= ISLAND_WINDOW_BLOCKS / 2;
                        dz += ISLAND_STRIDE_BLOCKS) {
                    int blockX = ISLAND_X + dx;
                    int blockZ = ISLAND_Z + dz;
                    int vanillaTop = topSolid(vanillaDensity, blockX, blockZ, seaLevel);
                    int cutTop = topSolid(cutDensity, blockX, blockZ, seaLevel);
                    if (vanillaTop == Integer.MIN_VALUE) {
                        continue;
                    }

                    counts[0]++;
                    double ceilingY = at(ceiling, blockX, blockZ);
                    if (vanillaTop <= ceilingY) {
                        counts[1]++;
                        assertTrue(cutTop == vanillaTop,
                                "a column standing below the ceiling moved at x=" + blockX + " z=" + blockZ
                                        + ": top " + vanillaTop + " became " + cutTop + ", ceiling "
                                        + round(ceilingY));
                        continue;
                    }

                    counts[2]++;
                    drops.add((double) (vanillaTop - cutTop));
                    aboves.add(vanillaTop - ceilingY);
                    int midY = (int) Math.round((ceilingY + vanillaTop) / 2.0);
                    insides.add(vanillaDensity.compute(
                            new DensityFunction.SinglePointContext(blockX, midY, blockZ)));
                }
            }
        });

        double[] sorted = drops.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        double[] above = aboves.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        long grazing = Arrays.stream(above).filter(over -> over <= GRAZING_BLOCKS).count();
        long unmoved = Arrays.stream(sorted).filter(drop -> drop == 0.0).count();
        double[] inside = insides.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        long deep = Arrays.stream(sorted).filter(drop -> drop >= ISLAND_MIN_CUT_BLOCKS).count();
        report.add("columns read: " + counts[0]);
        report.add("columns standing below the ceiling, untouched: " + counts[1]);
        report.add("columns standing above the ceiling: " + counts[2]);
        report.add("of those, cut by at least " + ISLAND_MIN_CUT_BLOCKS + " blocks: " + deep);
        report.add("of those, standing no more than " + GRAZING_BLOCKS + " blocks over the ceiling: " + grazing);
        report.add("of those, the ceiling took nothing at all: " + unmoved + " (the penalty did not outweigh them)");
        if (above.length > 0) {
            report.add("how far over the ceiling they stood: min " + round(above[0]) + ", p50 "
                    + round(percentile(above, 0.50)) + ", p99 " + round(percentile(above, 0.99))
                    + ", max " + round(above[above.length - 1]) + " blocks");
        }

        if (inside.length > 0) {
            report.add("vanilla density halfway between the ceiling and the top: p50 "
                    + round(percentile(inside, 0.50)) + ", p99 " + round(percentile(inside, 0.99))
                    + ", max " + round(inside[inside.length - 1]) + " (what the penalty has to outweigh)");
        }

        if (sorted.length > 0) {
            report.add("drop where the ceiling bit: min " + round(sorted[0]) + ", p50 "
                    + round(percentile(sorted, 0.50)) + ", max " + round(sorted[sorted.length - 1])
                    + " blocks");
        }

        write(ISLAND_REPORT, report);
        assertTrue(deep >= ISLAND_MIN_CUT_COLUMNS,
                "the known island did not go: " + deep + " columns lost " + ISLAND_MIN_CUT_BLOCKS
                        + " blocks or more, floor " + ISLAND_MIN_CUT_COLUMNS);
    }
}
