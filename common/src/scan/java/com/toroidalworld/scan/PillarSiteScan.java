package com.toroidalworld.scan;

import static com.toroidalworld.noise.ClimateScanFixture.randomState;
import static com.toroidalworld.noise.ClimateScanFixture.settingsOf;
import static com.toroidalworld.noise.ClimateScanFixture.torusOfWidth;
import static com.toroidalworld.scan.TerrainCeilingScan.withCeilingParked;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.ClimateScanFixture;
import com.toroidalworld.noise.PreliminarySurfaceLevel;
import com.toroidalworld.noise.ClimateScanFixture.WorldType;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.TerrainCeiling;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

class PillarSiteScan {
    private static final String TYPE = "default";

    private static final long SEED = 9059185195617760112L;

    private static final int WIDTH_BLOCKS = 512;

    private static final int SITE_X = 6;

    private static final int SITE_Z = -188;

    private static final int WINDOW = 48;

    private static final int MIN_Y = -64;

    private static final int HEIGHT = 384;

    private static final int LISTED_MASSES = 8;

    private static final Path REPORTS =
            Path.of(System.getProperty("toroidal.reports", "build/reports")).resolve("scan");

    private static final Path REPORT = REPORTS.resolve("terrain-ceiling-pillar.txt");

    private record Mass(int blocks, int baseY, int topY) {
    }

    @BeforeAll
    static void bootstrap() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void whatTheDensityFieldHoldsWhereThePillarStands() {
        WorldType type = ClimateScanFixture.TYPES.stream()
                .filter(candidate -> candidate.name().equals(TYPE))
                .findFirst()
                .orElseThrow();
        WorldFold fold = torusOfWidth(WIDTH_BLOCKS);
        NoiseGeneratorSettings vanilla = settingsOf(type);
        DensityFunction rawCeiling = TerrainCeiling.ceiling(vanilla);
        assertTrue(rawCeiling != null, "no ceiling for the " + TYPE + " settings");

        RandomState probeState = randomState(withCeilingParked(vanilla, rawCeiling), fold, SEED);
        RandomState cutState = randomState(TerrainCeiling.withCeiling(vanilla), fold, SEED);
        DensityFunction ceiling = probeState.router().barrierNoise();
        DensityFunction surface =
                new PreliminarySurfaceLevel(probeState.router().initialDensityWithoutJaggedness());

        List<String> report = new ArrayList<>();
        report.add("Pillar site — seed " + SEED + ", " + TYPE + ", lap " + WIDTH_BLOCKS + " blocks");
        report.add("density field only: no carvers, no aquifer, no surface rules, no features");
        report.add("window " + WINDOW + " blocks around x=" + SITE_X + " z=" + SITE_Z);
        report.add("");

        GenerationTransformerContext.runWithTransformer(fold, () -> {
            report.add("at x=" + SITE_X + " z=" + SITE_Z + ": preliminary surface y="
                    + round(at(surface)) + ", ceiling y=" + round(at(ceiling)));
            report.add("");
            report.addAll(masses("without the ceiling", probeState.router().finalDensity()));
            report.add("");
            report.addAll(masses("with the ceiling", cutState.router().finalDensity()));
        });

        write(report);
    }

    private static List<String> masses(String label, DensityFunction density) {
        boolean[] solid = new boolean[WINDOW * WINDOW * HEIGHT];
        int solidBlocks = 0;
        for (int dx = 0; dx < WINDOW; dx++) {
            for (int dz = 0; dz < WINDOW; dz++) {
                int blockX = SITE_X - WINDOW / 2 + dx;
                int blockZ = SITE_Z - WINDOW / 2 + dz;
                for (int y = 0; y < HEIGHT; y++) {
                    if (density.compute(new DensityFunction.SinglePointContext(blockX, MIN_Y + y, blockZ)) > 0.0) {
                        solid[index(dx, dz, y)] = true;
                        solidBlocks++;
                    }
                }
            }
        }

        List<Mass> detached = new ArrayList<>();
        boolean[] taken = new boolean[solid.length];
        int[] pending = new int[Math.max(1, solidBlocks)];
        int againstSide = 0;
        int grounded = 0;

        for (int start = 0; start < solid.length; start++) {
            if (!solid[start] || taken[start]) {
                continue;
            }

            taken[start] = true;
            pending[0] = start;
            int head = 0;
            int tail = 1;
            int blocks = 0;
            int lowest = Integer.MAX_VALUE;
            int highest = Integer.MIN_VALUE;
            boolean side = false;

            while (head < tail) {
                int cell = pending[head++];
                blocks++;
                int dx = cell % WINDOW;
                int dz = cell / WINDOW % WINDOW;
                int y = cell / (WINDOW * WINDOW);
                lowest = Math.min(lowest, y);
                highest = Math.max(highest, y);
                if (dx == 0 || dz == 0 || dx == WINDOW - 1 || dz == WINDOW - 1) {
                    side = true;
                }

                for (int axis = 0; axis < 3; axis++) {
                    for (int step = -1; step <= 1; step += 2) {
                        int nx = axis == 0 ? dx + step : dx;
                        int nz = axis == 1 ? dz + step : dz;
                        int ny = axis == 2 ? y + step : y;
                        if (nx < 0 || nz < 0 || ny < 0 || nx >= WINDOW || nz >= WINDOW || ny >= HEIGHT) {
                            continue;
                        }

                        int neighbour = index(nx, nz, ny);
                        if (solid[neighbour] && !taken[neighbour]) {
                            taken[neighbour] = true;
                            pending[tail++] = neighbour;
                        }
                    }
                }
            }

            if (lowest == 0) {
                grounded += blocks;
            } else if (side) {
                againstSide++;
            } else {
                detached.add(new Mass(blocks, MIN_Y + lowest, MIN_Y + highest));
            }
        }

        List<String> lines = new ArrayList<>();
        lines.add(label + ": " + solidBlocks + " solid blocks, " + grounded + " of them connected to the floor");
        lines.add("  detached masses: " + detached.size() + ", against a window side: " + againstSide);
        detached.stream()
                .sorted((left, right) -> right.blocks() - left.blocks())
                .limit(LISTED_MASSES)
                .forEach(mass -> lines.add("  mass: " + mass.blocks() + " blocks, y=" + mass.baseY()
                        + " to y=" + mass.topY()));
        return lines;
    }

    private static int index(int dx, int dz, int dy) {
        return dx + dz * WINDOW + dy * WINDOW * WINDOW;
    }

    private static double at(DensityFunction function) {
        return function.compute(new DensityFunction.SinglePointContext(SITE_X, 0, SITE_Z));
    }

    private static String round(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static void write(List<String> lines) {
        try {
            Files.createDirectories(REPORTS);
            Files.write(REPORT, lines);
        } catch (IOException failed) {
            throw new UncheckedIOException(failed);
        }
    }
}
