package com.toroidalworld.scan;

import static com.toroidalworld.noise.ClimateScanFixture.SCAN_Y_BLOCKS;
import static com.toroidalworld.noise.ClimateScanFixture.SEED_BASE;
import static com.toroidalworld.noise.ClimateScanFixture.TYPES;
import static com.toroidalworld.noise.ClimateScanFixture.biomeSource;
import static com.toroidalworld.noise.ClimateScanFixture.cylinderOfWidth;
import static com.toroidalworld.noise.ClimateScanFixture.randomState;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.noise.ClimateScanFixture;
import com.toroidalworld.noise.ClimateScanFixture.WorldType;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.options.WorldLoopPresets;

import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;

class ClimateScan {
    private static final int GRID = 64;
    private static final int SEEDS = 3;
    private static final int AXIS_SEEDS = 16;
    private static final long SEED_STEP = 0x9E3779B97F4A7C15L;

    private static final double MAX_TOP_SHARE = 0.45;

    private static final int UNBOUNDED_SPAN_BLOCKS = 8192;

    private static final int CONTROL_LINE_SPREAD_BLOCKS = 65536;

    private static final double MAX_ZONE_DRIFT = 0.20;

    private static final double MAX_SPREAD_DRIFT = 0.10;

    private static final Path REPORTS = Path.of(System.getProperty("toroidal.scan.reports", "build/reports/scan"));

    private static final Path REPORT = REPORTS.resolve("climate-scan.txt");

    private static final Path AXIS_REPORT = REPORTS.resolve("climate-cylinder-axis.txt");

    private record Shape(String name, IntFunction<WorldFold> foldOfWidth, boolean compressed) {
    }

    private static final List<Shape> SHAPES = List.of(
            new Shape("torus", ClimateScanFixture::torusOfWidth, true),
            new Shape("torus, strong", ClimateScanFixture::strongTorusOfWidth, true),
            new Shape("torus, uncompressed", ClimateScanFixture::uncompressedTorusOfWidth, false),
            new Shape("cylinder", ClimateScanFixture::cylinderOfWidth, false));

    private record Scan(double distinctBiomes, double topShare, double temperatureSpread) {
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
                .append("The nether is the overworld width divided by the preset's nether scale, and carries five")
                .append(" biomes in all, so it is reported and not gated.").append(System.lineSeparator())
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
                report.append(String.format("    %-8s %-14s %25s %25s%n", "preset", "width", "folded", "control"));

                for (WorldLoopPresets preset : WorldLoopPresets.values()) {
                    int widthBlocks = type.widthBlocks(preset);
                    Scan folded = meanScan(type, source, widthBlocks, shape.foldOfWidth().apply(widthBlocks));
                    Scan control = controls.computeIfAbsent(widthBlocks,
                            width -> meanScan(type, source, width, WorldFolds.NOOP));

                    report.append(String.format(
                            "    %-8s %-14s %6.1f biomes %5.2f %6.3f %6.1f biomes %5.2f %6.3f%n",
                            preset.id(), widthBlocks + " blocks",
                            folded.distinctBiomes(), folded.topShare(), folded.temperatureSpread(),
                            control.distinctBiomes(), control.topShare(), control.temperatureSpread()));

                    if (control.distinctBiomes() <= 1.0) {
                        thin.add(type.name() + " " + preset.id());
                    }

                    if (type.gated() && shape.compressed() && folded.topShare() > MAX_TOP_SHARE) {
                        dominated.add(String.format("%s %s %s at %.2f",
                                type.name(), shape.name(), preset.id(), folded.topShare()));
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
            report.append(String.format("    %-8s %-14s %22s %22s%n", "preset", "width", "cylinder", "control"));

            for (WorldLoopPresets preset : WorldLoopPresets.values()) {
                int widthBlocks = type.widthBlocks(preset);
                AxisScan folded = meanAlongZ(type, source, widthBlocks, cylinderOfWidth(widthBlocks));
                AxisScan control = meanAlongZ(type, source, CONTROL_LINE_SPREAD_BLOCKS, WorldFolds.NOOP);

                report.append(String.format("    %-8s %-14s %6.2f biomes %8.4f %6.2f biomes %8.4f%n",
                        preset.id(), widthBlocks + " blocks",
                        folded.distinctBiomes(), folded.temperatureSpread(),
                        control.distinctBiomes(), control.temperatureSpread()));

                double drift = Math.abs(folded.distinctBiomes() - control.distinctBiomes())
                        / control.distinctBiomes();
                if (drift > MAX_ZONE_DRIFT) {
                    off.add(String.format("%s %s zones off by %.0f%%", type.name(), preset.id(), drift * 100));
                }

                double spreadDrift = Math.abs(folded.temperatureSpread() - control.temperatureSpread())
                        / control.temperatureSpread();
                if (spreadDrift > MAX_SPREAD_DRIFT) {
                    off.add(String.format("%s %s spread off by %.0f%%", type.name(), preset.id(), spreadDrift * 100));
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

        for (int s = 0; s < SEEDS; s++) {
            Scan scan = scan(type, source, widthBlocks, fold, SEED_BASE + s * SEED_STEP);
            distinct += scan.distinctBiomes();
            topShare += scan.topShare();
            temperatureSpread += scan.temperatureSpread();
        }

        return new Scan(distinct / SEEDS, topShare / SEEDS, temperatureSpread / SEEDS);
    }

    private static Scan scan(WorldType type, MultiNoiseBiomeSource source, int widthBlocks, WorldFold fold,
            long seed) {
        Climate.Sampler sampler = randomState(type, fold, seed).sampler();
        int quartY = QuartPos.fromBlock(SCAN_Y_BLOCKS);
        double step = widthBlocks / (double) GRID;
        Map<Holder<Biome>, Integer> counts = new HashMap<>();
        double[] temperatures = new double[GRID * GRID];

        GenerationTransformerContext.runWithTransformer(fold, () -> {
            for (int ix = 0; ix < GRID; ix++) {
                for (int iz = 0; iz < GRID; iz++) {
                    int quartX = QuartPos.fromBlock((int) Math.round(ix * step));
                    int quartZ = QuartPos.fromBlock((int) Math.round(iz * step));
                    counts.merge(source.getNoiseBiome(quartX, quartY, quartZ, sampler), 1, Integer::sum);
                    temperatures[ix * GRID + iz] =
                            Climate.unquantizeCoord(sampler.sample(quartX, quartY, quartZ).temperature());
                }
            }
        });

        int samples = GRID * GRID;
        int top = counts.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return new Scan(counts.size(), top / (double) samples, standardDeviation(temperatures));
    }

    private static double standardDeviation(double[] values) {
        double mean = 0.0;
        for (double value : values) {
            mean += value;
        }

        mean /= values.length;
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
