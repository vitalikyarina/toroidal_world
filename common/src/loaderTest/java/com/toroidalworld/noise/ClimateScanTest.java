package com.toroidalworld.noise;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopPresets;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class ClimateScanTest {
    private static final int GRID = 64;
    private static final int SEEDS = 3;
    private static final long SEED_BASE = 0x5EED5EED5L;
    private static final long SEED_STEP = 0x9E3779B97F4A7C15L;

    private static final int SCAN_Y_BLOCKS = 64;

    private static final double MAX_TOP_SHARE = 0.45;

    private static final Path REPORT = Path.of("build", "reports", "climate-scan.txt");

    private static HolderLookup.Provider holders;
    private static HolderGetter<NormalNoise.NoiseParameters> noises;

    private record WorldType(String name, ResourceKey<NoiseGeneratorSettings> settings,
            ResourceKey<MultiNoiseBiomeSourceParameterList> biomes, boolean nether, boolean gated) {

        int widthBlocks(WorldLoopPresets preset) {
            return this.nether ? preset.blockWidth() / preset.netherScale() : preset.blockWidth();
        }
    }

    private static final List<WorldType> TYPES = List.of(
            new WorldType("default", NoiseGeneratorSettings.OVERWORLD,
                    MultiNoiseBiomeSourceParameterLists.OVERWORLD, false, true),
            new WorldType("large biomes", NoiseGeneratorSettings.LARGE_BIOMES,
                    MultiNoiseBiomeSourceParameterLists.OVERWORLD, false, true),
            new WorldType("amplified", NoiseGeneratorSettings.AMPLIFIED,
                    MultiNoiseBiomeSourceParameterLists.OVERWORLD, false, true),
            new WorldType("nether", NoiseGeneratorSettings.NETHER,
                    MultiNoiseBiomeSourceParameterLists.NETHER, true, false));

    private record Scan(double distinctBiomes, double topShare, double temperatureSpread) {
    }

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        holders = VanillaRegistries.createLookup();
        noises = holders.lookupOrThrow(Registries.NOISE);
    }

    private static MultiNoiseBiomeSource biomeSource(WorldType type) {
        Holder<MultiNoiseBiomeSourceParameterList> preset = holders
                .lookupOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST).getOrThrow(type.biomes());
        return MultiNoiseBiomeSource.createFromPreset(preset);
    }

    private static WorldFold foldOfWidth(int widthBlocks) {
        return WorldFolds.of(
                FlatShape.latticeTorus(WorldLoopBounds.ofWidth(widthBlocks / 16), FlatShape.NO_SKEW));
    }

    private static RandomState randomState(WorldType type, WorldFold fold, long seed) {
        NoiseGeneratorSettings settings =
                holders.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(type.settings()).value();
        return GenerationTransformerContext.withRouterBuild(fold.isWrapped() ? fold : null,
                () -> RandomState.create(settings, noises, seed));
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
                .append(" biomes in all, so it is reported and not gated.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        List<String> thin = new ArrayList<>();
        List<String> dominated = new ArrayList<>();

        for (WorldType type : TYPES) {
            MultiNoiseBiomeSource source = biomeSource(type);
            report.append("  ").append(type.name()).append(System.lineSeparator());
            report.append(String.format("    %-8s %-14s %25s %25s%n", "preset", "width", "folded", "control"));

            for (WorldLoopPresets preset : WorldLoopPresets.values()) {
                int widthBlocks = type.widthBlocks(preset);
                Scan folded = meanScan(type, source, widthBlocks, foldOfWidth(widthBlocks));
                Scan control = meanScan(type, source, widthBlocks, WorldFolds.NOOP);

                report.append(String.format("    %-8s %-14s %6.1f biomes %5.2f %6.3f %6.1f biomes %5.2f %6.3f%n",
                        preset.id(), widthBlocks + " blocks",
                        folded.distinctBiomes(), folded.topShare(), folded.temperatureSpread(),
                        control.distinctBiomes(), control.topShare(), control.temperatureSpread()));

                if (control.distinctBiomes() <= 1.0) {
                    thin.add(type.name() + " " + preset.id());
                }

                if (type.gated() && folded.topShare() > MAX_TOP_SHARE) {
                    dominated.add(String.format("%s %s at %.2f", type.name(), preset.id(), folded.topShare()));
                }
            }

            report.append(System.lineSeparator());
        }

        write(report.toString());

        assertTrue(thin.isEmpty(),
                "the control window itself carries no biome spread, so the scan measures nothing: " + thin);
        assertTrue(dominated.isEmpty(), "a folded preset is dominated by one biome: " + dominated);
    }

    @Test
    void theScanSamplesAWorldThatActuallyRepeatsOneWidthAway() {
        WorldType type = TYPES.getFirst();
        MultiNoiseBiomeSource source = biomeSource(type);
        int width = WorldLoopPresets.TINY.blockWidth();
        WorldFold fold = foldOfWidth(width);
        Climate.Sampler sampler = randomState(type, fold, SEED_BASE).sampler();
        int quartY = QuartPos.fromBlock(SCAN_Y_BLOCKS);
        Map<String, Integer> broken = new HashMap<>();

        GenerationTransformerContext.runWithTransformer(fold, () -> {
            for (int i = 0; i < GRID; i++) {
                int x = i * (width / GRID);
                int z = (i * 37) % width;
                Climate.TargetPoint here = sampler.sample(QuartPos.fromBlock(x), quartY, QuartPos.fromBlock(z));
                Climate.TargetPoint lapAway =
                        sampler.sample(QuartPos.fromBlock(x + width), quartY, QuartPos.fromBlock(z));
                collect(broken, "temperature", here.temperature(), lapAway.temperature());
                collect(broken, "humidity", here.humidity(), lapAway.humidity());
                collect(broken, "continentalness", here.continentalness(), lapAway.continentalness());
                collect(broken, "erosion", here.erosion(), lapAway.erosion());
                collect(broken, "depth", here.depth(), lapAway.depth());
                collect(broken, "weirdness", here.weirdness(), lapAway.weirdness());
                source.getNoiseBiome(QuartPos.fromBlock(x), quartY, QuartPos.fromBlock(z), sampler);
            }
        });

        assertTrue(broken.isEmpty(), "the scan is not sampling a folded world, mismatches of " + GRID
                + " samples per field: " + broken);
    }

    private static void collect(Map<String, Integer> broken, String field, long here, long lapAway) {
        if (here != lapAway) {
            broken.merge(field, 1, Integer::sum);
        }
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

    private static void write(String report) {
        try {
            Files.createDirectories(REPORT.getParent());
            Files.writeString(REPORT, report);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
