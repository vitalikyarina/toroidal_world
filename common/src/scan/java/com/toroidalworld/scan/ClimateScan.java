package com.toroidalworld.scan;

import static com.toroidalworld.noise.ClimateScanFixture.SCAN_Y_BLOCKS;
import static com.toroidalworld.noise.ClimateScanFixture.SEED_BASE;
import static com.toroidalworld.noise.ClimateScanFixture.TYPES;
import static com.toroidalworld.noise.ClimateScanFixture.biomeSource;
import static com.toroidalworld.noise.ClimateScanFixture.cylinderOfWidth;
import static com.toroidalworld.noise.ClimateScanFixture.guaranteedTorusOfWidth;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.CoordinateConstants;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.noise.ClimateScanFixture;
import com.toroidalworld.noise.ClimateScanFixture.WorldType;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.options.NetherScales;
import com.toroidalworld.options.WorldLoopPresets;
import com.toroidalworld.options.WorldLoopSizes;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.RandomState;

class ClimateScan {
    private static final int GRID = 64;
    private static final int LAND_GRID = 32;
    private static final int SEEDS = 3;
    private static final int AXIS_SEEDS = 16;
    private static final int LAND_SEEDS = 32;
    private static final long SEED_STEP = 0x9E3779B97F4A7C15L;

    private static final double MAX_TOP_SHARE = 0.45;

    private static final double USABLE_LAND_SHARE = 0.20;

    private static final int PATCH_STRIDE_BLOCKS = 16;

    private static final int PATCH_GRID_CAP = 128;

    private static final int PATCH_MAX_WIDTH_BLOCKS = 4096;

    private static final long LAND_PATCH_FLOOR_BLOCKS = 4096L;

    private static final double OCEAN_CONTINENTALNESS = -0.19;

    private static final int[][] PATCH_NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private static final int UNBOUNDED_SPAN_BLOCKS = 8192;

    private static final int CONTROL_LINE_SPREAD_BLOCKS = 65536;

    private static final double MAX_ZONE_DRIFT = 0.20;

    private static final double MAX_SPREAD_DRIFT = 0.10;

    private static final Path REPORTS = Path.of(System.getProperty("toroidal.reports", "build/reports")).resolve("scan");

    private static final Path REPORT = REPORTS.resolve("climate-scan.txt");

    private static final Path AXIS_REPORT = REPORTS.resolve("climate-cylinder-axis.txt");

    private static final Path LAND_REPORT = REPORTS.resolve("land-scan.txt");

    private static final Path PATCH_REPORT = REPORTS.resolve("land-patch-scan.txt");

    private record Width(String id, int chunkWidth, int netherScale) {
        int widthBlocks(WorldType type) {
            int blocks = this.chunkWidth * CoordinateConstants.CHUNK_WIDTH;
            return type.nether()
                    ? blocks / NetherScales.normalize(this.netherScale, this.chunkWidth)
                    : blocks;
        }
    }

    private static final List<Width> WIDTHS = widths();

    private static List<Width> widths() {
        List<Width> widths = new ArrayList<>();
        widths.add(new Width("min", WorldLoopSizes.MIN_CHUNK_WIDTH, NetherScales.DEFAULT));
        for (WorldLoopPresets preset : WorldLoopPresets.values()) {
            widths.add(new Width(preset.id(), preset.chunkWidth(), preset.netherScale()));
        }

        return List.copyOf(widths);
    }

    private record Shape(String name, IntFunction<WorldFold> foldOfWidth, boolean compressed) {
    }

    private static final List<Shape> SHAPES = List.of(
            new Shape("torus", ClimateScanFixture::torusOfWidth, true),
            new Shape("torus, strong", ClimateScanFixture::strongTorusOfWidth, true),
            new Shape("torus, uncompressed", ClimateScanFixture::uncompressedTorusOfWidth, false),
            new Shape("cylinder", ClimateScanFixture::cylinderOfWidth, false));

    private record Scan(double distinctBiomes, double topShare, double temperatureSpread, double landShare) {
    }

    private record AxisScan(double distinctBiomes, double temperatureSpread) {
    }

    @BeforeAll
    static void bootstrapVanilla() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void measuresTheBiomeSpreadOfEveryPresetThroughTheRealClimateSampler() {
        StringBuilder report = new StringBuilder();
        report.append("Climate scan - vanilla Climate.Sampler through the real router, biomes from")
                .append(" MultiNoiseBiomeSource, no chunk generation.").append(System.lineSeparator())
                .append("Grid ").append(GRID).append("x").append(GRID).append(" points over one lap at y=")
                .append(SCAN_Y_BLOCKS).append(" blocks, ").append(SEEDS).append(" seeds, mean.")
                .append(System.lineSeparator())
                .append("Folded and control runs are the same code - the control binds WorldFolds.NOOP, so it")
                .append(" measures the same window of an unbounded vanilla world.").append(System.lineSeparator())
                .append("top share = area fraction of the most common biome; 1.00 is a one-biome world.")
                .append(" spread = standard deviation of the temperature field.").append(System.lineSeparator())
                .append("land = area fraction whose final density is solid at the world type's sea level, so the")
                .append(" surface there stands above the water rather than under it.")
                .append(System.lineSeparator())
                .append("min is the narrowest world the game will create; the nether is the overworld width")
                .append(" divided by the nether scale that width allows, and carries five biomes in all, so it is")
                .append(" reported and not gated.").append(System.lineSeparator())
                .append("A torus that declined compression and a cylinder are never compressed, so one lap of")
                .append(" either is vanilla's own window of that size; the one-biome gate applies to the")
                .append(" compressed torus alone.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        List<String> thin = new ArrayList<>();
        List<String> dominated = new ArrayList<>();

        for (WorldType type : TYPES) {
            MultiNoiseBiomeSource source = biomeSource(type);
            Map<Integer, Scan> controls = new HashMap<>();

            for (Shape shape : SHAPES) {
                report.append("  ").append(type.name()).append(", ").append(shape.name())
                        .append(System.lineSeparator());
                report.append(String.format("    %-8s %-14s %33s %33s%n", "width", "blocks", "folded", "control"));

                for (Width width : WIDTHS) {
                    int widthBlocks = width.widthBlocks(type);
                    Scan folded = meanScan(type, source, widthBlocks, shape.foldOfWidth().apply(widthBlocks));
                    Scan control = controls.computeIfAbsent(widthBlocks,
                            blocks -> meanScan(type, source, blocks, WorldFolds.NOOP));

                    report.append(String.format(
                            "    %-8s %-14s %6.1f biomes %5.2f %6.3f %6.2f %6.1f biomes %5.2f %6.3f %6.2f%n",
                            width.id(), widthBlocks + " blocks",
                            folded.distinctBiomes(), folded.topShare(), folded.temperatureSpread(),
                            folded.landShare(),
                            control.distinctBiomes(), control.topShare(), control.temperatureSpread(),
                            control.landShare()));

                    if (control.distinctBiomes() <= 1.0) {
                        thin.add(type.name() + " " + width.id());
                    }

                    if (type.gated() && shape.compressed() && folded.topShare() > MAX_TOP_SHARE) {
                        dominated.add(String.format("%s %s %s at %.2f",
                                type.name(), shape.name(), width.id(), folded.topShare()));
                    }
                }

                report.append(System.lineSeparator());
            }
        }

        write(REPORT, report.toString());

        assertTrue(thin.isEmpty(),
                "the control window itself carries no biome spread, so the scan measures nothing: " + thin);
        assertTrue(dominated.isEmpty(), "a folded preset is dominated by one biome: " + dominated);
    }

    @Test
    void reportsHowOftenALapComesOutWithNowhereToStand() {
        StringBuilder report = new StringBuilder();
        report.append("Land scan - vanilla final density at the world type's sea level through the real router,")
                .append(" no chunk generation.").append(System.lineSeparator())
                .append("Grid ").append(LAND_GRID).append("x").append(LAND_GRID).append(" points over one lap, ")
                .append(LAND_SEEDS).append(" seeds per row.").append(System.lineSeparator())
                .append("land = area fraction standing above the water; a cave at sea level reads as water, so")
                .append(" the share is the low estimate.").append(System.lineSeparator())
                .append("The control binds WorldFolds.NOOP, so it measures the same window of an unbounded")
                .append(" vanilla world - the nether has no sea level to stand on and is left out.")
                .append(System.lineSeparator())
                .append("under = seeds whose land share falls below ")
                .append(String.format("%.0f%%", USABLE_LAND_SHARE * 100))
                .append("; best seed is the one to hand a round that needs land.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        List<String> blind = new ArrayList<>();

        for (WorldType type : TYPES) {
            if (type.nether()) {
                continue;
            }

            report.append("  ").append(type.name()).append(System.lineSeparator());
            report.append(String.format("    %-8s %-14s %17s %17s%n", "", "", "torus", "control"));
            report.append(String.format("    %-8s %-14s %5s %5s %5s %5s %5s %5s %20s%n",
                    "width", "blocks", "med", "min", "under", "med", "min", "under", "best seed"));

            for (Width width : WIDTHS) {
                int widthBlocks = width.widthBlocks(type);
                Land torus = landDistribution(type, widthBlocks, torusOfWidth(widthBlocks));
                Land control = landDistribution(type, widthBlocks, WorldFolds.NOOP);

                report.append(String.format("    %-8s %-14s %5.2f %5.2f %5d %5.2f %5.2f %5d %20d%n",
                        width.id(), widthBlocks + " blocks",
                        torus.median(), torus.min(), torus.under(),
                        control.median(), control.min(), control.under(),
                        torus.bestSeed()));

                if (control.under() == LAND_SEEDS) {
                    blind.add(type.name() + " " + width.id());
                }
            }

            report.append(System.lineSeparator());
        }

        write(LAND_REPORT, report.toString());

        assertTrue(blind.isEmpty(),
                "no control seed clears the land floor, so the scan measures nothing: " + blind);
    }

    private record Land(double median, double min, int under, long bestSeed) {
    }

    private static Land landDistribution(WorldType type, int widthBlocks, WorldFold fold) {
        double[] shares = new double[LAND_SEEDS];
        long bestSeed = SEED_BASE;
        double best = -1.0;
        int under = 0;

        for (int s = 0; s < LAND_SEEDS; s++) {
            long seed = SEED_BASE + s * SEED_STEP;
            shares[s] = landShare(type, fold, widthBlocks, seed);
            if (shares[s] > best) {
                best = shares[s];
                bestSeed = seed;
            }

            if (shares[s] < USABLE_LAND_SHARE) {
                under++;
            }
        }

        double[] sorted = shares.clone();
        Arrays.sort(sorted);
        double median = (sorted[(sorted.length - 1) / 2] + sorted[sorted.length / 2]) / 2.0;
        return new Land(median, sorted[0], under, bestSeed);
    }

    private static double landShare(WorldType type, WorldFold fold, int widthBlocks, long seed) {
        DensityFunction density = randomState(type, fold, seed).router().finalDensity();
        int seaLevel = settingsOf(type).seaLevel();
        double step = widthBlocks / (double) LAND_GRID;
        int[] land = new int[1];

        GenerationTransformerContext.runWithTransformer(fold, () -> {
            for (int ix = 0; ix < LAND_GRID; ix++) {
                for (int iz = 0; iz < LAND_GRID; iz++) {
                    if (isLand(density, (int) Math.round(ix * step), seaLevel, (int) Math.round(iz * step))) {
                        land[0]++;
                    }
                }
            }
        });

        return land[0] / (double) (LAND_GRID * LAND_GRID);
    }

    @Test
    void reportsTheLargestPatchOfLandOnALap() {
        StringBuilder report = new StringBuilder();
        report.append("Largest land patch - the biggest run of land connected across the lap, seam included,")
                .append(" off the same final density at sea level as the land scan.")
                .append(System.lineSeparator())
                .append("Sampled every ").append(PATCH_STRIDE_BLOCKS).append(" blocks up to a ")
                .append(PATCH_GRID_CAP).append("x").append(PATCH_GRID_CAP)
                .append(" grid, so the step is one chunk while it fits and coarser above; ")
                .append(LAND_SEEDS).append(" seeds per row, torus only.").append(System.lineSeparator())
                .append("Areas are blocks squared; empty = seeds whose lap carries no land at all.")
                .append(System.lineSeparator())
                .append("Widths past ").append(PATCH_MAX_WIDTH_BLOCKS)
                .append(" blocks are left out - no seed there falls under the land floor.")
                .append(System.lineSeparator())
                .append("corr = Pearson correlation between the lap's mean continentalness and its patch area.")
                .append(" fail/pass are the highest mean still under the ")
                .append(LAND_PATCH_FLOOR_BLOCKS).append(" blocks² floor and the lowest mean clearing it, so")
                .append(" they bracket the mean a lift has to reach; vanilla puts ocean under ")
                .append(OCEAN_CONTINENTALNESS).append(".").append(System.lineSeparator())
                .append("spread = mean of the per-seed standard deviation of continentalness over the lap.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        List<String> blind = new ArrayList<>();
        List<String> unguarded = new ArrayList<>();

        for (WorldType type : TYPES) {
            if (type.nether()) {
                continue;
            }

            report.append("  ").append(type.name()).append(System.lineSeparator());
            report.append(String.format("    %-8s %-14s %25s %25s %7s%n", "", "", "off", "on", ""));
            report.append(String.format("    %-8s %-14s %12s %10s %4s %12s %10s %4s %7s%n",
                    "width", "blocks", "med", "min", "bad", "med", "min", "bad", "corr"));

            for (Width width : WIDTHS) {
                int widthBlocks = width.widthBlocks(type);
                if (widthBlocks > PATCH_MAX_WIDTH_BLOCKS) {
                    continue;
                }

                Patch off = patchOf(probeLaps(type, widthBlocks, torusOfWidth(widthBlocks)));
                Patch on = patchOf(probeLaps(type, widthBlocks, guaranteedTorusOfWidth(widthBlocks)));

                report.append(String.format("    %-8s %-14s %12.0f %10d %4d %12.0f %10d %4d %7s%n",
                        width.id(), widthBlocks + " blocks",
                        off.median(), off.min(), off.bad(),
                        on.median(), on.min(), on.bad(),
                        cell(off.correlation(), "%.2f")));

                if (off.max() == 0L) {
                    blind.add(type.name() + " " + width.id());
                }

                if (on.bad() > 0) {
                    unguarded.add(String.format("%s %s %d/%d", type.name(), width.id(), on.bad(), LAND_SEEDS));
                }
            }

            report.append(System.lineSeparator());
        }

        write(PATCH_REPORT, report.toString());

        assertTrue(blind.isEmpty(), "no seed of a row carries any land, so the scan measures nothing: " + blind);
        assertTrue(unguarded.isEmpty(),
                "the guarantee left seeds under " + LAND_PATCH_FLOOR_BLOCKS + " blocks squared: " + unguarded);
    }

    private record Patch(double median, long min, long max, int bad, double correlation) {
    }

    private static Patch patchOf(LapProbe[] probes) {
        long[] sorted = new long[probes.length];
        double[] means = new double[probes.length];
        double[] areas = new double[probes.length];
        int bad = 0;

        for (int i = 0; i < probes.length; i++) {
            sorted[i] = probes[i].patchBlocks();
            means[i] = probes[i].continentalnessMean();
            areas[i] = probes[i].patchBlocks();
            if (probes[i].patchBlocks() < LAND_PATCH_FLOOR_BLOCKS) {
                bad++;
            }
        }

        Arrays.sort(sorted);
        double median = (sorted[(sorted.length - 1) / 2] + sorted[sorted.length / 2]) / 2.0;
        return new Patch(median, sorted[0], sorted[sorted.length - 1], bad, correlation(means, areas));
    }

    private record LapProbe(long patchBlocks, double continentalnessMean, double continentalnessSpread) {
    }

    private static LapProbe[] probeLaps(WorldType type, int widthBlocks, WorldFold fold) {
        LapProbe[] probes = new LapProbe[LAND_SEEDS];

        for (int s = 0; s < LAND_SEEDS; s++) {
            probes[s] = probeLap(type, fold, widthBlocks, SEED_BASE + s * SEED_STEP);
        }

        return probes;
    }

    private static LapProbe probeLap(WorldType type, WorldFold fold, int widthBlocks, long seed) {
        int stride = Math.max(PATCH_STRIDE_BLOCKS, widthBlocks / PATCH_GRID_CAP);
        int grid = widthBlocks / stride;
        RandomState randomState = randomState(type, fold, seed);
        DensityFunction density = randomState.router().finalDensity();
        DensityFunction continents = randomState.router().continents();
        int seaLevel = settingsOf(type).seaLevel();
        boolean[] land = new boolean[grid * grid];
        double[] field = new double[grid * grid];

        GenerationTransformerContext.runWithTransformer(fold, () -> {
            for (int ix = 0; ix < grid; ix++) {
                for (int iz = 0; iz < grid; iz++) {
                    int blockX = ix * stride;
                    int blockZ = iz * stride;
                    land[ix * grid + iz] = isLand(density, blockX, seaLevel, blockZ);
                    field[ix * grid + iz] =
                            continents.compute(new DensityFunction.SinglePointContext(blockX, seaLevel, blockZ));
                }
            }
        });

        return new LapProbe((long) largestComponent(land, grid) * stride * stride, mean(field),
                standardDeviation(field));
    }

    private static String cell(double value, String format) {
        return Double.isNaN(value) ? "n/a" : String.format(format, value);
    }

    private static double correlation(double[] xs, double[] ys) {
        double meanX = mean(xs);
        double meanY = mean(ys);
        double covariance = 0.0;
        double varianceX = 0.0;
        double varianceY = 0.0;

        for (int i = 0; i < xs.length; i++) {
            double dx = xs[i] - meanX;
            double dy = ys[i] - meanY;
            covariance += dx * dy;
            varianceX += dx * dx;
            varianceY += dy * dy;
        }

        return varianceX == 0.0 || varianceY == 0.0 ? Double.NaN : covariance / Math.sqrt(varianceX * varianceY);
    }

    private static int largestComponent(boolean[] land, int grid) {
        boolean[] seen = new boolean[land.length];
        int[] queue = new int[land.length];
        int largest = 0;

        for (int start = 0; start < land.length; start++) {
            if (!land[start] || seen[start]) {
                continue;
            }

            seen[start] = true;
            queue[0] = start;
            int head = 0;
            int tail = 1;

            while (head < tail) {
                int cell = queue[head++];
                int x = cell / grid;
                int z = cell % grid;

                for (int[] step : PATCH_NEIGHBOURS) {
                    int next = Math.floorMod(x + step[0], grid) * grid + Math.floorMod(z + step[1], grid);
                    if (land[next] && !seen[next]) {
                        seen[next] = true;
                        queue[tail++] = next;
                    }
                }
            }

            largest = Math.max(largest, tail);
        }

        return largest;
    }

    @Test
    void theCylinderCarriesVanillaClimateAlongItsUnboundedAxis() {
        StringBuilder report = new StringBuilder();
        report.append("Cylinder, climate along the unbounded axis - the axis that carries no lap and so is")
                .append(" starved of nothing.").append(System.lineSeparator())
                .append(GRID).append(" lines, spread across the ring and across ")
                .append(CONTROL_LINE_SPREAD_BLOCKS).append(" blocks for the control, which has no ring; each is ")
                .append(GRID).append(" points over ").append(UNBOUNDED_SPAN_BLOCKS).append(" blocks of Z at y=")
                .append(SCAN_Y_BLOCKS).append(" blocks, ").append(AXIS_SEEDS).append(" seeds, mean per line.")
                .append(System.lineSeparator())
                .append("The ring's own variation never enters a line, so this is the unbounded axis alone,")
                .append(" against the same measure taken on an unbounded vanilla world.")
                .append(System.lineSeparator())
                .append("Zones per line is the scale measure and is gated at ")
                .append(String.format("%.0f%%", MAX_ZONE_DRIFT * 100))
                .append("; the spread is gated at ").append(String.format("%.0f%%", MAX_SPREAD_DRIFT * 100))
                .append(" - the ring's rule cannot move a line, so this is the estimator's own floor and")
                .append(" catches a compression-class regression, not the ring.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        List<String> off = new ArrayList<>();

        for (WorldType type : TYPES) {
            MultiNoiseBiomeSource source = biomeSource(type);
            report.append("  ").append(type.name()).append(System.lineSeparator());
            report.append(String.format("    %-8s %-14s %22s %22s%n", "width", "blocks", "cylinder", "control"));

            for (Width width : WIDTHS) {
                int widthBlocks = width.widthBlocks(type);
                AxisScan folded = meanAlongZ(type, source, widthBlocks, cylinderOfWidth(widthBlocks));
                AxisScan control = meanAlongZ(type, source, CONTROL_LINE_SPREAD_BLOCKS, WorldFolds.NOOP);

                report.append(String.format("    %-8s %-14s %6.2f biomes %8.4f %6.2f biomes %8.4f%n",
                        width.id(), widthBlocks + " blocks",
                        folded.distinctBiomes(), folded.temperatureSpread(),
                        control.distinctBiomes(), control.temperatureSpread()));

                double drift = Math.abs(folded.distinctBiomes() - control.distinctBiomes())
                        / control.distinctBiomes();
                if (drift > MAX_ZONE_DRIFT) {
                    off.add(String.format("%s %s zones off by %.0f%%", type.name(), width.id(), drift * 100));
                }

                double spreadDrift = Math.abs(folded.temperatureSpread() - control.temperatureSpread())
                        / control.temperatureSpread();
                if (spreadDrift > MAX_SPREAD_DRIFT) {
                    off.add(String.format("%s %s spread off by %.0f%%", type.name(), width.id(), spreadDrift * 100));
                }
            }

            report.append(System.lineSeparator());
        }

        write(AXIS_REPORT, report.toString());

        assertTrue(off.isEmpty(), "the unbounded axis does not carry vanilla's zone size or spread: " + off);
    }

    private static AxisScan meanAlongZ(WorldType type, MultiNoiseBiomeSource source, int lineSpreadBlocks,
            WorldFold fold) {
        double distinct = 0.0;
        double spread = 0.0;

        for (int s = 0; s < AXIS_SEEDS; s++) {
            AxisScan scan = alongZ(type, source, lineSpreadBlocks, fold, SEED_BASE + s * SEED_STEP);
            distinct += scan.distinctBiomes();
            spread += scan.temperatureSpread();
        }

        return new AxisScan(distinct / AXIS_SEEDS, spread / AXIS_SEEDS);
    }

    private static AxisScan alongZ(WorldType type, MultiNoiseBiomeSource source, int lineSpreadBlocks,
            WorldFold fold, long seed) {
        Climate.Sampler sampler = randomState(type, fold, seed).sampler();
        int quartY = QuartPos.fromBlock(SCAN_Y_BLOCKS);
        double xStep = lineSpreadBlocks / (double) GRID;
        double zStep = UNBOUNDED_SPAN_BLOCKS / (double) GRID;
        double[] totals = new double[2];

        GenerationTransformerContext.runWithTransformer(fold, () -> {
            double[] temperatures = new double[GRID];

            for (int ix = 0; ix < GRID; ix++) {
                int quartX = QuartPos.fromBlock((int) Math.round(ix * xStep));
                Set<Holder<Biome>> biomes = new HashSet<>();

                for (int iz = 0; iz < GRID; iz++) {
                    int quartZ = QuartPos.fromBlock((int) Math.round(iz * zStep));
                    biomes.add(source.getNoiseBiome(quartX, quartY, quartZ, sampler));
                    temperatures[iz] = Climate.unquantizeCoord(sampler.sample(quartX, quartY, quartZ).temperature());
                }

                totals[0] += biomes.size();
                totals[1] += standardDeviation(temperatures);
            }
        });

        return new AxisScan(totals[0] / GRID, totals[1] / GRID);
    }

    private static Scan meanScan(WorldType type, MultiNoiseBiomeSource source, int widthBlocks, WorldFold fold) {
        double distinct = 0.0;
        double topShare = 0.0;
        double temperatureSpread = 0.0;
        double landShare = 0.0;

        for (int s = 0; s < SEEDS; s++) {
            Scan scan = scan(type, source, widthBlocks, fold, SEED_BASE + s * SEED_STEP);
            distinct += scan.distinctBiomes();
            topShare += scan.topShare();
            temperatureSpread += scan.temperatureSpread();
            landShare += scan.landShare();
        }

        return new Scan(distinct / SEEDS, topShare / SEEDS, temperatureSpread / SEEDS, landShare / SEEDS);
    }

    private static Scan scan(WorldType type, MultiNoiseBiomeSource source, int widthBlocks, WorldFold fold,
            long seed) {
        RandomState randomState = randomState(type, fold, seed);
        Climate.Sampler sampler = randomState.sampler();
        DensityFunction density = randomState.router().finalDensity();
        int seaLevel = settingsOf(type).seaLevel();
        int quartY = QuartPos.fromBlock(SCAN_Y_BLOCKS);
        double step = widthBlocks / (double) GRID;
        Map<Holder<Biome>, Integer> counts = new HashMap<>();
        double[] temperatures = new double[GRID * GRID];
        int[] land = new int[1];

        GenerationTransformerContext.runWithTransformer(fold, () -> {
            for (int ix = 0; ix < GRID; ix++) {
                for (int iz = 0; iz < GRID; iz++) {
                    int blockX = (int) Math.round(ix * step);
                    int blockZ = (int) Math.round(iz * step);
                    int quartX = QuartPos.fromBlock(blockX);
                    int quartZ = QuartPos.fromBlock(blockZ);
                    counts.merge(source.getNoiseBiome(quartX, quartY, quartZ, sampler), 1, Integer::sum);
                    temperatures[ix * GRID + iz] =
                            Climate.unquantizeCoord(sampler.sample(quartX, quartY, quartZ).temperature());
                    if (isLand(density, blockX, seaLevel, blockZ)) {
                        land[0]++;
                    }
                }
            }
        });

        int samples = GRID * GRID;
        int top = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return new Scan(counts.size(), top / (double) samples, standardDeviation(temperatures),
                land[0] / (double) samples);
    }

    private static boolean isLand(DensityFunction density, int blockX, int seaLevel, int blockZ) {
        return density.compute(new DensityFunction.SinglePointContext(blockX, seaLevel, blockZ)) > 0.0;
    }

    private static double mean(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }

        return sum / values.length;
    }

    private static double standardDeviation(double[] values) {
        double mean = mean(values);
        double sum = 0.0;
        for (double value : values) {
            sum += (value - mean) * (value - mean);
        }

        return Math.sqrt(sum / values.length);
    }

    private static void write(Path path, String report) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, report);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
