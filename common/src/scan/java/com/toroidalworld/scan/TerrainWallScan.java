package com.toroidalworld.scan;

import static com.toroidalworld.noise.ClimateScanFixture.TYPES;
import static com.toroidalworld.noise.ClimateScanFixture.noiseParameters;
import static com.toroidalworld.noise.ClimateScanFixture.randomState;
import static com.toroidalworld.noise.ClimateScanFixture.settingsOf;
import static com.toroidalworld.noise.ClimateScanFixture.torusOfWidth;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.noise.ClimateFields;
import com.toroidalworld.noise.ClimateScaleCompression;
import com.toroidalworld.noise.ClimateScanFixture;
import com.toroidalworld.noise.ClimateScanFixture.WorldType;
import com.toroidalworld.noise.GenerationTransformerContext;
import com.toroidalworld.noise.OctaveVarianceCorrection;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.Noises;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

class TerrainWallScan {
    private static final long WALL_SEED = -4241666765127210365L;

    private static final int WIDTH_BLOCKS = 512;

    private static final int CELL_WIDTH = 4;

    private static final int CELL_HEIGHT = 8;

    private static final int LEVELS_BELOW_SEA = 1;

    private static final int LEVELS_ABOVE_SEA = 4;

    private static final double BLADE_BLOCKS = 1.5;

    private static final int RIDGE_CORNERS = 3;

    private static final double WIDE = CELL_WIDTH * 2.0;

    private static final int WORST_SITES = 12;

    private static final Path REPORT = ScanReports.DIRECTORY.resolve("terrain-wall-scan.txt");

    private static final Path OCTAVE_REPORT = ScanReports.DIRECTORY.resolve("terrain-wall-octaves.txt");

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

    @BeforeAll
    static void bootstrapVanilla() {
        ClimateScanFixture.bootstrapVanilla();
    }

    @Test
    void locatesTheThinTerrainWallsOfACompactedLap() {
        WorldType type = TYPES.getFirst();
        Pass folded = pass("torus " + WIDTH_BLOCKS + " blocks", type, torusOfWidth(WIDTH_BLOCKS));
        Pass control = pass("control (unbounded vanilla)", type, WorldFolds.NOOP);

        StringBuilder report = new StringBuilder();
        report.append("Terrain wall scan - final density on the generator's own cell lattice, seed ")
                .append(WALL_SEED).append(", world type ").append(type.name()).append(".")
                .append(System.lineSeparator())
                .append("The generator interpolates final density over ").append(CELL_WIDTH).append("x")
                .append(CELL_HEIGHT).append("x").append(CELL_WIDTH)
                .append("-block cells, so a corner grid of that step carries the whole shape of the terrain.")
                .append(System.lineSeparator())
                .append("thickness = width in blocks of the solid band around a corner, from the linear crossing")
                .append(" to its two neighbours along one axis; a blade is under ")
                .append(String.format("%.1f", BLADE_BLOCKS))
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
                .append(System.lineSeparator()).append(System.lineSeparator());

        for (Pass pass : List.of(folded, control)) {
            appendPass(report, pass);
        }

        ScanReports.write(REPORT, report.toString());

        assertTrue(control.landShare() > 0.0 && control.landShare() < 1.0,
                "the control window is all water or all land, so the scan measures nothing: " + control.landShare());
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
                .append("compression = ClimateScaleCompression.factor under Compact biomes Auto.")
                .append(System.lineSeparator())
                .append("cells = cells the octave carries over one lap; period = the lattice it closes on,")
                .append(" floored to 4 where the natural period falls under 2.").append(System.lineSeparator())
                .append("damp = OctaveVarianceCorrection.factor, gain = its anchor gain; an octave the floor")
                .append(" never caught takes damp 1 and gain 0.")
                .append(System.lineSeparator()).append(System.lineSeparator());

        for (ResourceKey<NormalNoise.NoiseParameters> key : TERRAIN_FIELDS) {
            appendField(report, key, xDomain, zDomain, fold);
        }

        ScanReports.write(OCTAVE_REPORT, report.toString());
    }

    private static void appendField(StringBuilder report, ResourceKey<NormalNoise.NoiseParameters> key,
            WrapDomain xDomain, WrapDomain zDomain, WorldFold fold) {
        NormalNoise.NoiseParameters parameters = noiseParameters(key);
        DoubleList amplitudes = parameters.amplitudes();
        double lowestFreqInputFactor = Math.pow(2.0, parameters.firstOctave());
        double compression = ClimateScaleCompression.factor(fold, ClimateFields.isClimate(key), amplitudes,
                lowestFreqInputFactor, CLIMATE_XZ_SCALE, HORIZONTAL_SHARE);

        report.append("  ").append(key.identifier().getPath())
                .append(String.format(", first octave %d, compression %.3f%n",
                        parameters.firstOctave(), compression));
        report.append(String.format("    %-7s %10s %9s %8s %7s %7s%n",
                "octave", "amplitude", "cells", "period", "damp", "gain"));

        for (int i = 0; i < amplitudes.size(); i++) {
            double scale = CLIMATE_XZ_SCALE * compression * lowestFreqInputFactor * Math.pow(2.0, i);
            long natural = Math.round(WIDTH_BLOCKS * scale);
            report.append(String.format("    %-7d %10.3f %9.4f %8s %7.3f %7.3f%n",
                    i, amplitudes.getDouble(i), WIDTH_BLOCKS * scale,
                    natural < 2L ? natural + " -> 4" : Long.toString(natural),
                    OctaveVarianceCorrection.factor(xDomain, zDomain, scale, HORIZONTAL_SHARE),
                    OctaveVarianceCorrection.anchorGain(xDomain, zDomain, scale, HORIZONTAL_SHARE)));
        }

        report.append(System.lineSeparator());
    }

    private static void appendPass(StringBuilder report, Pass pass) {
        report.append("  ").append(pass.name()).append(System.lineSeparator());
        report.append(String.format("    land share %.3f, blades %d, per level %s%n",
                pass.landShare(), pass.blades().size(), levels(pass.bladesPerLevel())));
        report.append(String.format("    %-16s %9s %9s %9s %9s %10s %10s%n",
                "field", "mean", "spread", "min", "max", "grad mean", "grad max"));

        for (Field field : pass.fields()) {
            report.append(String.format("    %-16s %9.4f %9.4f %9.4f %9.4f %10.4f %10.4f%n",
                    field.name(), field.mean(), field.spread(), field.min(), field.max(),
                    field.gradientMean(), field.gradientMax()));
        }

        if (!pass.blades().isEmpty()) {
            report.append(String.format("    %-22s %9s %9s %10s %9s %9s %9s%n",
                    "thinnest blades", "thick", "density", "continents", "erosion", "ridges", "depth"));

            for (Site site : pass.blades().stream()
                    .sorted(Comparator.comparingDouble(Site::thickness))
                    .limit(WORST_SITES)
                    .toList()) {
                report.append(String.format("    %-22s %9.2f %9.4f %10.4f %9.4f %9.4f %9.4f%n",
                        site.blockX() + " " + site.blockY() + " " + site.blockZ(),
                        site.thickness(), site.density(), site.continents(), site.erosion(), site.ridges(),
                        site.depth()));
            }
        }

        report.append(System.lineSeparator());
    }

    private static Pass pass(String name, WorldType type, WorldFold fold) {
        RandomState randomState = randomState(type, fold, WALL_SEED);
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
