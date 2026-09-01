package com.toroidalworld.noise;

import com.toroidalworld.core.WrapDomain;

import it.unimi.dsi.fastutil.doubles.DoubleList;

public final class ClimateScaleCompression {
    static final double CELLS_PER_LAP = 1.5;

    private static final double HORIZONTAL_SHARE = 0.0;

    private static final double NO_COMPRESSION = 1.0;

    public static double factor(WrapDomain xDomain, WrapDomain zDomain, DoubleList amplitudes,
            double lowestFreqInputFactor, double baseScale, double verticalShare) {
        if (verticalShare != HORIZONTAL_SHARE) {
            return NO_COMPRESSION;
        }

        if (!xDomain.loops() || !zDomain.loops()) {
            return NO_COMPRESSION;
        }

        int lap = Math.min(xDomain.domainLength, zDomain.domainLength);
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

    private ClimateScaleCompression() {
    }
}
