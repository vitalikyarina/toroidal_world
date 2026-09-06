package com.toroidalworld.noise;

import java.util.ArrayList;
import java.util.List;

import com.toroidalworld.accessors.CoastLiftCache;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WrapDomain;

import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
import net.minecraft.world.level.levelgen.DensityFunction.Visitor;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;

public final class CoastFieldLift {
    static final long LAND_FLOOR_BLOCKS = 4096L;

    private static final double[] CANDIDATES = {0.0, 0.05, 0.1, 0.15, 0.2, 0.3, 0.4, 0.6, 0.8, 1.0, 1.3, 1.6};

    private static final int STRIDE_BLOCKS = 16;

    private static final int GRID_CAP = 64;

    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    public static void solve(RandomState randomState, WorldFold fold, int seaLevel) {
        if (!fold.generationOptions().guaranteedLand()) {
            return;
        }

        WrapDomain xDomain = fold.blockDomain(Direction.Axis.X);
        WrapDomain zDomain = fold.blockDomain(Direction.Axis.Z);
        if (!xDomain.loops() || !zDomain.loops()) {
            return;
        }

        List<CoastLiftCache> coasts = coastNoises(randomState.router());
        if (coasts.isEmpty()) {
            return;
        }

        DensityFunction density = randomState.router().finalDensity();
        int stride = Math.max(STRIDE_BLOCKS,
                Math.max(xDomain.domainLength, zDomain.domainLength) / GRID_CAP);
        int xGrid = Math.max(1, xDomain.domainLength / stride);
        int zGrid = Math.max(1, zDomain.domainLength / stride);
        long cellBlocks = (long) stride * stride;

        for (double candidate : CANDIDATES) {
            apply(coasts, candidate);
            long patch = GenerationTransformerContext.withTransformer(fold,
                    () -> largestPatch(density, seaLevel, stride, xGrid, zGrid)) * cellBlocks;
            if (patch >= LAND_FLOOR_BLOCKS) {
                return;
            }
        }

        apply(coasts, CANDIDATES[CANDIDATES.length - 1]);
    }

    private static void apply(List<CoastLiftCache> coasts, double lift) {
        for (CoastLiftCache coast : coasts) {
            coast.toroidal$coastLift(lift);
        }
    }

    private static List<CoastLiftCache> coastNoises(NoiseRouter router) {
        List<CoastLiftCache> coasts = new ArrayList<>();
        router.continents().mapAll(new Visitor() {
            @Override
            public DensityFunction apply(DensityFunction input) {
                return input;
            }

            @Override
            public NoiseHolder visitNoise(NoiseHolder noise) {
                if (noise.noise() instanceof CoastLiftCache coast
                        && noise.noiseData().unwrapKey().filter(CoastFields::isCoast).isPresent()) {
                    coasts.add(coast);
                }

                return noise;
            }
        });

        return coasts;
    }

    private static int largestPatch(DensityFunction density, int seaLevel, int stride, int xGrid, int zGrid) {
        boolean[] land = new boolean[xGrid * zGrid];
        for (int ix = 0; ix < xGrid; ix++) {
            for (int iz = 0; iz < zGrid; iz++) {
                land[ix * zGrid + iz] = density.compute(
                        new DensityFunction.SinglePointContext(ix * stride, seaLevel, iz * stride)) > 0.0;
            }
        }

        return largestComponent(land, xGrid, zGrid);
    }

    private static int largestComponent(boolean[] land, int xGrid, int zGrid) {
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
                int x = cell / zGrid;
                int z = cell % zGrid;

                for (int[] step : NEIGHBOURS) {
                    int next = Math.floorMod(x + step[0], xGrid) * zGrid + Math.floorMod(z + step[1], zGrid);
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

    private CoastFieldLift() {
    }
}
