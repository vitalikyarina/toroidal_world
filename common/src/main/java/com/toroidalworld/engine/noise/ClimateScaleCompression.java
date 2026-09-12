package com.toroidalworld.engine.noise;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WrapDomain;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;

public final class ClimateScaleCompression {
    public static final double NO_COMPRESSION = 1.0;

    public static final double CELLS_PER_LAP = 1.5;

    private static final double HORIZONTAL_SHARE = 0.0;

    private static final int UNBOUNDED_LAP = 0;

    public static boolean compressible(WorldFold fold, double verticalShare) {
        return verticalShare == HORIZONTAL_SHARE && lapBlocks(fold) != UNBOUNDED_LAP;
    }

    public static double fitted(WorldFold fold, DoubleList amplitudes, double lowestFreqInputFactor,
            double baseScale) {
        return fittedFactor(amplitudes, lapBlocks(fold) * baseScale * lowestFreqInputFactor);
    }

    private static int lapBlocks(WorldFold fold) {
        WrapDomain xDomain = fold.blockDomain(Direction.Axis.X);
        WrapDomain zDomain = fold.blockDomain(Direction.Axis.Z);
        return xDomain.loops() && zDomain.loops()
                ? Math.min(xDomain.domainLength, zDomain.domainLength)
                : UNBOUNDED_LAP;
    }

    private static double fittedFactor(DoubleList amplitudes, double lowestOctaveCells) {
        double cellsPerLap = weightedCellsPerLap(amplitudes, lowestOctaveCells);
        return cellsPerLap <= 0.0 || cellsPerLap >= CELLS_PER_LAP
                ? NO_COMPRESSION
                : CELLS_PER_LAP / cellsPerLap;
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
