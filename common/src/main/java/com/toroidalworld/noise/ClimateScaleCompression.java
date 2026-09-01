package com.toroidalworld.noise;

import com.toroidalworld.core.WrapDomain;

import it.unimi.dsi.fastutil.doubles.DoubleList;

public final class ClimateScaleCompression {
    static final double CELLS_PER_LAP = 1.5;

    private static final double HORIZONTAL_SHARE = 0.0;

    private static final double NO_COMPRESSION = 1.0;

    private static final int UNBOUNDED_LAP = 0;

    public static double factor(WrapDomain xDomain, WrapDomain zDomain, DoubleList amplitudes,
            double lowestFreqInputFactor, double baseScale, double verticalShare) {
        if (verticalShare != HORIZONTAL_SHARE) {
            return NO_COMPRESSION;
        }

        int lap = shortestLap(xDomain, zDomain);
        if (lap == UNBOUNDED_LAP) {
            return NO_COMPRESSION;
        }

        double cellsPerLap = weightedCellsPerLap(amplitudes, lap * baseScale * lowestFreqInputFactor);
        if (cellsPerLap <= 0.0 || cellsPerLap >= CELLS_PER_LAP) {
            return NO_COMPRESSION;
        }

        return CELLS_PER_LAP / cellsPerLap;
    }

    private static double weightedCellsPerLap(DoubleList amplitudes, double lowestOctaveCells) {
        double weighted = 0.0;
        double weight = 0.0;
        double cells = lowestOctaveCells;

        for (int i = 0; i < amplitudes.size(); i++) {
            double amplitude = amplitudes.getDouble(i);
            if (amplitude != 0.0) {
                double square = amplitude * amplitude;
                weighted += square * cells;
                weight += square;
            }

            cells *= 2.0;
        }

        return weight == 0.0 ? 0.0 : weighted / weight;
    }

    private static int shortestLap(WrapDomain xDomain, WrapDomain zDomain) {
        int x = lapOf(xDomain);
        int z = lapOf(zDomain);
        if (x == UNBOUNDED_LAP) {
            return z;
        }

        if (z == UNBOUNDED_LAP) {
            return x;
        }

        return Math.min(x, z);
    }

    private static int lapOf(WrapDomain domain) {
        return domain instanceof WrapDomain.Noop ? UNBOUNDED_LAP : domain.domainLength;
    }

    private ClimateScaleCompression() {
    }
}
