package com.toroidalworld.scan;

import static com.toroidalworld.noise.ClimateScanFixture.SEED_BASE;
import static com.toroidalworld.noise.ClimateScanFixture.randomState;
import static com.toroidalworld.noise.ClimateScanFixture.settingsOf;
import static com.toroidalworld.noise.ClimateScanFixture.torusOfWidth;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.noise.ClimateScanFixture;
import com.toroidalworld.noise.ClimateScanFixture.WorldType;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.TerrainCeiling;

import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;

final class SuspendedLand {
    static final int WIDTH_BLOCKS = 512;

    static final int TOP_Y = 319;

    static final int WINDOW_BLOCKS = 96;

    static final int STRIDE_BLOCKS = 2;

    static final int SUSPENDED_BLOCKS = 30;

    static final int SITE_COLUMNS = 16;

    private static final int SEEDS = 16;

    private static final long SEED_STEP = 0x9E3779B97F4A7C15L;

    private static final int SWEEP_STEP_BLOCKS = 8;

    private static final int CLUSTERS = 4;

    private static final int FLOOR_Y = 32;

    private static final int COARSE_STEP_BLOCKS = 4;

    private static @Nullable Site found;

    private static @Nullable String swept;

    private static @Nullable String closest;

    record Site(WorldType type, long seed, int blockX, int blockZ, int columns) {
        String describe() {
            return type.name() + ", seed " + seed + ", x=" + blockX + " z=" + blockZ + ", lap "
                    + WIDTH_BLOCKS + " blocks, " + columns + " columns of it standing " + SUSPENDED_BLOCKS
                    + " blocks or more above the ceiling";
        }
    }

    private record Hit(int blockX, int blockZ, int overshoot) {
    }

    private record Sweep(@Nullable Site site, @Nullable Hit highest) {
    }

    static synchronized Site requireSite() {
        if (swept == null) {
            search();
        }

        Site site = found;
        assertTrue(site != null, "no land stands " + SUSPENDED_BLOCKS + " blocks above the ceiling: "
                + swept + " searched, " + closest + " — the ceiling has nothing to cut here, so this scan"
                + " cannot tell a working ceiling from a broken one");
        return site;
    }

    static long seed(int index) {
        return SEED_BASE + index * SEED_STEP;
    }

    static int topSolid(DensityFunction density, int blockX, int blockZ, int seaLevel) {
        return topSolidAbove(density, blockX, blockZ, Math.min(FLOOR_Y, seaLevel));
    }

    private static int topSolidAbove(DensityFunction density, int blockX, int blockZ, int floor) {
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

    static double at(DensityFunction function, int blockX, int blockZ) {
        return function.compute(new DensityFunction.SinglePointContext(blockX, 0, blockZ));
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

    private static void search() {
        List<String> types = new ArrayList<>();
        Hit highest = null;
        String highestAt = "";
        for (WorldType type : ClimateScanFixture.TYPES) {
            NoiseGeneratorSettings vanilla = settingsOf(type);
            DensityFunction rawCeiling = TerrainCeiling.ceiling(vanilla);
            if (rawCeiling == null) {
                continue;
            }

            types.add(type.name());
            NoiseGeneratorSettings probe = withCeilingParked(vanilla, rawCeiling);
            WorldFold fold = torusOfWidth(WIDTH_BLOCKS);
            for (int index = 0; index < SEEDS && found == null; index++) {
                long seed = seed(index);
                RandomState state = randomState(probe, fold, seed);
                DensityFunction ceiling = state.router().barrierNoise();
                DensityFunction density = state.router().finalDensity();
                Sweep[] result = new Sweep[1];
                GenerationTransformerContext.runWithTransformer(fold,
                        () -> result[0] = sweep(type, seed, ceiling, density, vanilla.seaLevel()));
                found = result[0].site();
                Hit reach = result[0].highest();
                if (reach != null && (highest == null || reach.overshoot() > highest.overshoot())) {
                    highest = reach;
                    highestAt = type.name() + " seed " + seed + " x=" + reach.blockX()
                            + " z=" + reach.blockZ();
                }
            }
        }

        swept = SEEDS + " seeds of " + String.join(" and ", types) + ", one column every "
                + SWEEP_STEP_BLOCKS + " blocks over a " + WIDTH_BLOCKS + "-block lap";
        closest = highest == null
                ? "no column stands above the ceiling at all"
                : "the highest column the sweep found stood " + highest.overshoot()
                        + " blocks above it, at " + highestAt;
    }

    private static Sweep sweep(WorldType type, long seed, DensityFunction ceiling, DensityFunction density,
            int seaLevel) {
        List<Hit> hits = new ArrayList<>();
        Hit reach = null;
        for (int blockX = -WIDTH_BLOCKS / 2; blockX < WIDTH_BLOCKS / 2; blockX += SWEEP_STEP_BLOCKS) {
            for (int blockZ = -WIDTH_BLOCKS / 2; blockZ < WIDTH_BLOCKS / 2; blockZ += SWEEP_STEP_BLOCKS) {
                int overshoot = overshoot(ceiling, density, blockX, blockZ);
                if (overshoot == Integer.MIN_VALUE) {
                    continue;
                }

                Hit hit = new Hit(blockX, blockZ, overshoot);
                if (reach == null || overshoot > reach.overshoot()) {
                    reach = hit;
                }

                if (overshoot >= SUSPENDED_BLOCKS) {
                    hits.add(hit);
                }
            }
        }

        for (int cluster = 0; cluster < CLUSTERS && !hits.isEmpty(); cluster++) {
            Hit centre = densest(hits);
            int columns = suspendedColumns(ceiling, density, centre, seaLevel);
            if (columns >= SITE_COLUMNS) {
                return new Sweep(new Site(type, seed, centre.blockX(), centre.blockZ(), columns), reach);
            }

            hits.removeIf(hit -> within(centre, hit));
        }

        return new Sweep(null, reach);
    }

    private static int overshoot(DensityFunction ceiling, DensityFunction density, int blockX, int blockZ) {
        double ceilingY = at(ceiling, blockX, blockZ);
        int top = topSolidAbove(density, blockX, blockZ, (int) Math.floor(ceilingY) + 1);
        return top == Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) Math.floor(top - ceilingY);
    }

    private static Hit densest(List<Hit> hits) {
        Hit densest = hits.get(0);
        int best = -1;
        for (Hit centre : hits) {
            int neighbours = 0;
            for (Hit hit : hits) {
                if (within(centre, hit)) {
                    neighbours++;
                }
            }

            if (neighbours > best) {
                best = neighbours;
                densest = centre;
            }
        }

        return densest;
    }

    private static boolean within(Hit centre, Hit hit) {
        return Math.abs(hit.blockX() - centre.blockX()) <= WINDOW_BLOCKS / 2
                && Math.abs(hit.blockZ() - centre.blockZ()) <= WINDOW_BLOCKS / 2;
    }

    private static int suspendedColumns(DensityFunction ceiling, DensityFunction density, Hit centre,
            int seaLevel) {
        int columns = 0;
        for (int dx = -WINDOW_BLOCKS / 2; dx <= WINDOW_BLOCKS / 2; dx += STRIDE_BLOCKS) {
            for (int dz = -WINDOW_BLOCKS / 2; dz <= WINDOW_BLOCKS / 2; dz += STRIDE_BLOCKS) {
                int blockX = centre.blockX() + dx;
                int blockZ = centre.blockZ() + dz;
                int top = topSolid(density, blockX, blockZ, seaLevel);
                if (top != Integer.MIN_VALUE && top - at(ceiling, blockX, blockZ) >= SUSPENDED_BLOCKS) {
                    columns++;
                }
            }
        }

        return columns;
    }

    private SuspendedLand() {
    }
}
