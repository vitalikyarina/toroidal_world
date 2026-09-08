package com.toroidalworld.scan;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.engine.noise.ClimateScanFixture;
import com.toroidalworld.engine.noise.GenerationTransformerContext;
import com.toroidalworld.engine.noise.TerrainCeiling;
import static com.toroidalworld.engine.noise.ClimateScanFixture.randomState;
import static com.toroidalworld.engine.noise.ClimateScanFixture.settingsOf;
import static com.toroidalworld.engine.noise.ClimateScanFixture.torusOfWidth;
import static com.toroidalworld.scan.SuspendedLand.WIDTH_BLOCKS;
import static com.toroidalworld.scan.SuspendedLand.at;
import static com.toroidalworld.scan.SuspendedLand.withCeilingParked;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;

class PillarSiteScan {
    private static final int WINDOW = 48;

    private static final int MIN_Y = -64;

    private static final int HEIGHT = 384;

    private static final int LISTED_MASSES = 8;

    private static final Path REPORT = ScanReports.DIRECTORY.resolve("terrain-ceiling-pillar.txt");

    private record Mass(int blocks, int baseY, int topY) {
    }

    @BeforeAll
    static void bootstrap() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void whatTheDensityFieldHoldsWhereThePillarStands() {
        SuspendedLand.Site site = SuspendedLand.requireSite();
        WorldFold fold = torusOfWidth(WIDTH_BLOCKS);
        NoiseGeneratorSettings vanilla = settingsOf(site.type());
        DensityFunction rawCeiling = TerrainCeiling.ceiling(vanilla);
        assertTrue(rawCeiling != null, "no ceiling for the " + site.type().name() + " settings");

        RandomState probeState = randomState(withCeilingParked(vanilla, rawCeiling), fold, site.seed());
        RandomState cutState = randomState(TerrainCeiling.withCeiling(vanilla), fold, site.seed());
        DensityFunction ceiling = probeState.router().barrierNoise();
        DensityFunction surface = probeState.router().preliminarySurfaceLevel();

        List<String> report = new ArrayList<>();
        report.add("Pillar site — " + site.describe());
        report.add("density field only: no carvers, no aquifer, no surface rules, no features");
        report.add("window " + WINDOW + " blocks around x=" + site.blockX() + " z=" + site.blockZ());
        report.add("");

        GenerationTransformerContext.runWithTransformer(fold, () -> {
            report.add("at x=" + site.blockX() + " z=" + site.blockZ() + ": preliminary surface y="
                    + round(at(surface, site.blockX(), site.blockZ())) + ", ceiling y="
                    + round(at(ceiling, site.blockX(), site.blockZ())));
            report.add("");
            report.addAll(masses("without the ceiling", probeState.router().finalDensity(), site));
            report.add("");
            report.addAll(masses("with the ceiling", cutState.router().finalDensity(), site));
        });

        ScanReports.write(REPORT, report);
    }

    private static List<String> masses(String label, DensityFunction density, SuspendedLand.Site site) {
        boolean[] solid = new boolean[WINDOW * WINDOW * HEIGHT];
        int solidBlocks = 0;
        for (int dx = 0; dx < WINDOW; dx++) {
            for (int dz = 0; dz < WINDOW; dz++) {
                int blockX = site.blockX() - WINDOW / 2 + dx;
                int blockZ = site.blockZ() - WINDOW / 2 + dz;
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

    private static String round(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
