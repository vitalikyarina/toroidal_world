package com.toroidalworld.noise;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.shape.FlatShape;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseSettings;
import net.minecraft.world.level.levelgen.RandomState;

class NetherColumnProbeTest {
    private static final int CHUNK_BLOCKS = 16;
    private static final int UNBOUNDED_CHUNKS = 0;
    private static final int UNBOUNDED_WINDOW_BLOCKS = 512;
    private static final int GRID = 16;
    private static final int SEEDS = 4;
    private static final long SEED_BASE = 0x5EED5EED5L;
    private static final long SEED_STEP = 0x9E3779B97F4A7C15L;
    private static final double VANILLA_WINDOW_SPAN = 1.0E6;
    private static final int TOP_LEVELS = 3;
    private static final double MAX_FOLDED_DRIFT = 0.15;

    private static HolderLookup.Provider holders;
    private static NoiseGeneratorSettings netherSettings;
    private static NoiseBasedChunkGenerator generator;
    private static LevelHeightAccessor heightAccessor;
    private static BlockState defaultBlock;

    private record Gate(String lap, String path, double control, double folded) {
    }

    private final List<Gate> gates = new ArrayList<>();

    @BeforeAll
    static void bootstrapVanilla() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        holders = VanillaRegistries.createLookup();
        Holder<NoiseGeneratorSettings> settingsHolder =
                holders.lookupOrThrow(Registries.NOISE_SETTINGS).getOrThrow(NoiseGeneratorSettings.NETHER);
        netherSettings = settingsHolder.value();
        generator = new NoiseBasedChunkGenerator(
                new FixedBiomeSource(holders.lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.NETHER_WASTES)),
                settingsHolder);
        NoiseSettings noiseSettings = netherSettings.noiseSettings();
        heightAccessor = LevelHeightAccessor.create(noiseSettings.minY(), noiseSettings.height());
        defaultBlock = netherSettings.defaultBlock();
    }

    private record Lap(String name, int xChunks, int zChunks) {
        WorldFold fold() {
            if (this.zChunks == UNBOUNDED_CHUNKS) {
                return WorldFolds.of(FlatShape.cylinder(
                        WorldLoopBounds.ofWidth(Direction.Axis.X, this.xChunks)));
            }

            if (this.xChunks == UNBOUNDED_CHUNKS) {
                return WorldFolds.of(FlatShape.cylinder(
                        WorldLoopBounds.ofWidth(Direction.Axis.Z, this.zChunks)));
            }

            return WorldFolds.of(FlatShape.torus(new WorldLoopBounds(
                    -(this.xChunks / 2), this.xChunks - this.xChunks / 2,
                    -(this.zChunks / 2), this.zChunks - this.zChunks / 2)));
        }

        int spanBlocks(int chunks) {
            return chunks == UNBOUNDED_CHUNKS ? UNBOUNDED_WINDOW_BLOCKS : chunks * CHUNK_BLOCKS;
        }

        int spanX() {
            return spanBlocks(this.xChunks);
        }

        int spanZ() {
            return spanBlocks(this.zChunks);
        }
    }

    private static final List<Lap> LAPS = List.of(
            new Lap("square 16x16 chunks (256x256 blocks)", 16, 16),
            new Lap("square 32x32 chunks (512x512 blocks)", 32, 32),
            new Lap("square 64x64 chunks (1024x1024 blocks)", 64, 64),
            new Lap("square 128x128 chunks (2048x2048 blocks)", 128, 128),
            new Lap("rect 16x64 chunks (256x1024 blocks)", 16, 64),
            new Lap("rect 64x16 chunks (1024x256 blocks)", 64, 16),
            new Lap("cylinder x=16 chunks (256 blocks), z unbounded", 16, UNBOUNDED_CHUNKS),
            new Lap("cylinder z=32 chunks (512 blocks), x unbounded", UNBOUNDED_CHUNKS, 32));

    @Test
    void measuresNetherColumnsThroughTheRealChain() {
        StringBuilder report = new StringBuilder();
        report.append("Nether column probe — vanilla NoiseBasedChunkGenerator.getBaseColumn over")
                .append(" NoiseGeneratorSettings.NETHER: full router, postProcess, cell interpolation (")
                .append(netherSettings.noiseSettings().getCellWidth()).append(" blocks wide x ")
                .append(netherSettings.noiseSettings().getCellHeight()).append(" tall), aquifer and fluid picker.\n")
                .append("chain = getBaseColumn, solid is the default block; density = router finalDensity > 0, no cells and no aquifer.\n")
                .append("Folded and control runs are the same code — the control binds WorldFolds.NOOP.\n\n");

        for (Lap lap : LAPS) {
            WorldFold fold = lap.fold();
            double stepX = lap.spanX() / (double) GRID;
            double stepZ = lap.spanZ() / (double) GRID;
            Random windows = new Random(SEED_BASE);
            report.append("  ").append(lap.name())
                    .append(String.format(" stride %.0fx%.0f blocks%n", stepX, stepZ));
            double vanillaTop1Sum = 0.0;
            double foldedTop1Sum = 0.0;
            double densityControlTop1Sum = 0.0;
            double densityFoldedTop1Sum = 0.0;
            for (int s = 0; s < SEEDS; s++) {
                long seed = SEED_BASE + s * SEED_STEP;
                RandomState randomState = RandomState.create(netherSettings,
                        holders.lookupOrThrow(Registries.NOISE), seed);
                int windowX = (int) (windows.nextDouble() * VANILLA_WINDOW_SPAN);
                int windowZ = (int) (windows.nextDouble() * VANILLA_WINDOW_SPAN);

                int foldedX = -lap.spanX() / 2;
                int foldedZ = -lap.spanZ() / 2;
                Relief control = measureRelief(randomState, WorldFolds.NOOP, windowX, windowZ,
                        stepX, stepZ, this::chainColumn);
                Relief folded = measureRelief(randomState, fold, foldedX, foldedZ,
                        stepX, stepZ, this::chainColumn);
                Relief densityControl = measureRelief(randomState, WorldFolds.NOOP, windowX, windowZ,
                        stepX, stepZ, this::densityColumn);
                Relief densityFolded = measureRelief(randomState, fold, foldedX, foldedZ,
                        stepX, stepZ, this::densityColumn);
                vanillaTop1Sum += control.floorTop1Share();
                foldedTop1Sum += folded.floorTop1Share();
                densityControlTop1Sum += densityControl.floorTop1Share();
                densityFoldedTop1Sum += densityFolded.floorTop1Share();
                report.append(String.format("    seed %d chain   control: %s%n", s, control.line()));
                report.append(String.format("    seed %d chain   folded:  %s%n", s, folded.line()));
                report.append(String.format("    seed %d density control: %s%n", s, densityControl.line()));
                report.append(String.format("    seed %d density folded:  %s%n", s, densityFolded.line()));
            }
            report.append(String.format("    mean floor_top1 chain:   control %.3f | folded %.3f%n",
                    vanillaTop1Sum / SEEDS, foldedTop1Sum / SEEDS));
            report.append(String.format("    mean floor_top1 density: control %.3f | folded %.3f%n",
                    densityControlTop1Sum / SEEDS, densityFoldedTop1Sum / SEEDS));
            this.gates.add(new Gate(lap.name(), "chain", vanillaTop1Sum / SEEDS, foldedTop1Sum / SEEDS));
            this.gates.add(new Gate(lap.name(), "density",
                    densityControlTop1Sum / SEEDS, densityFoldedTop1Sum / SEEDS));
        }

        Path out = Path.of("build", "reports", "nether-column-probe.txt");
        try {
            Files.createDirectories(out.getParent());
            Files.writeString(out, report.toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        for (Gate gate : this.gates) {
            double drift = Math.abs(gate.folded() - gate.control());
            assertTrue(drift <= MAX_FOLDED_DRIFT, String.format(
                    "%s (%s): folded floor_top1 %.3f drifted %.3f off the unfolded control %.3f",
                    gate.lap(), gate.path(), gate.folded(), drift, gate.control()));
        }
    }

    private record Relief(double floorTop1Share, double floorTop3Share, int distinctFloorLevels,
            double meanNeighbourDelta, double ceilingTop1Share, int distinctCeilingLevels, double solidShare) {
        String line() {
            return String.format(
                    "floor_top1=%.3f floor_top3=%.3f floor_levels=%d neighbour_delta=%.2f blocks"
                            + " ceiling_top1=%.3f ceiling_levels=%d solid_share=%.3f",
                    this.floorTop1Share, this.floorTop3Share, this.distinctFloorLevels, this.meanNeighbourDelta,
                    this.ceilingTop1Share, this.distinctCeilingLevels, this.solidShare);
        }
    }

    private interface ColumnSampler {
        boolean[] sample(RandomState randomState, int blockX, int blockZ);
    }

    private Relief measureRelief(RandomState randomState, WorldFold fold, int originX, int originZ,
            double stepX, double stepZ, ColumnSampler sampler) {
        int minY = heightAccessor.getMinY();
        int height = heightAccessor.getHeight();
        int[][] floors = new int[GRID][GRID];
        int[][] ceilings = new int[GRID][GRID];
        long solidBlocks = 0;

        for (int i = 0; i < GRID; i++) {
            int blockX = originX + (int) Math.round(i * stepX);
            for (int j = 0; j < GRID; j++) {
                int blockZ = originZ + (int) Math.round(j * stepZ);
                boolean[] solid = GenerationTransformerContext.withTransformer(fold,
                        () -> sampler.sample(randomState, blockX, blockZ));

                int floor = minY - 1;
                for (int y = 0; y < height && solid[y]; y++) {
                    floor = minY + y;
                }

                int ceiling = minY + height;
                for (int y = height - 1; y >= 0 && solid[y]; y--) {
                    ceiling = minY + y;
                }

                for (boolean block : solid) {
                    if (block) {
                        solidBlocks++;
                    }
                }

                floors[i][j] = floor;
                ceilings[i][j] = ceiling;
            }
        }

        double neighbourSum = 0.0;
        int neighbourCount = 0;
        for (int i = 0; i < GRID; i++) {
            for (int j = 0; j < GRID; j++) {
                if (i + 1 < GRID) {
                    neighbourSum += Math.abs(floors[i + 1][j] - floors[i][j]);
                    neighbourCount++;
                }

                if (j + 1 < GRID) {
                    neighbourSum += Math.abs(floors[i][j + 1] - floors[i][j]);
                    neighbourCount++;
                }
            }
        }

        Histogram floorHistogram = Histogram.of(floors, minY, height);
        Histogram ceilingHistogram = Histogram.of(ceilings, minY, height);
        return new Relief(floorHistogram.topShare(1), floorHistogram.topShare(TOP_LEVELS),
                floorHistogram.distinct(), neighbourSum / neighbourCount,
                ceilingHistogram.topShare(1), ceilingHistogram.distinct(),
                solidBlocks / (double) (GRID * GRID * height));
    }

    private boolean[] chainColumn(RandomState randomState, int blockX, int blockZ) {
        NoiseColumn column = generator.getBaseColumn(blockX, blockZ, heightAccessor, randomState);
        int minY = heightAccessor.getMinY();
        boolean[] solid = new boolean[heightAccessor.getHeight()];
        for (int i = 0; i < solid.length; i++) {
            solid[i] = column.getBlock(minY + i) == defaultBlock;
        }

        return solid;
    }

    private boolean[] densityColumn(RandomState randomState, int blockX, int blockZ) {
        DensityFunction density = randomState.router().finalDensity();
        int minY = heightAccessor.getMinY();
        boolean[] solid = new boolean[heightAccessor.getHeight()];
        for (int i = 0; i < solid.length; i++) {
            solid[i] = density.compute(new DensityFunction.SinglePointContext(blockX, minY + i, blockZ)) > 0.0;
        }

        return solid;
    }

    private record Histogram(int[] counts, int total) {
        static Histogram of(int[][] levels, int minY, int height) {
            int[] counts = new int[height + 2];
            int total = 0;
            for (int[] row : levels) {
                for (int level : row) {
                    counts[level - (minY - 1)]++;
                    total++;
                }
            }
            return new Histogram(counts, total);
        }

        double topShare(int take) {
            int[] sorted = this.counts.clone();
            Arrays.sort(sorted);
            int sum = 0;
            for (int i = 0; i < take; i++) {
                sum += sorted[sorted.length - 1 - i];
            }
            return sum / (double) this.total;
        }

        int distinct() {
            int distinct = 0;
            for (int count : this.counts) {
                if (count > 0) {
                    distinct++;
                }
            }
            return distinct;
        }
    }
}
