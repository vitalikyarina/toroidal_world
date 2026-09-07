package com.toroidalworld.noise;

import com.toroidalworld.accessors.ClimateCompressionCache;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.shape.torus.ClimateScale;
import com.toroidalworld.shape.torus.CompactBiomes;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;

public final class ClimateScaleCompression {
    static final double CELLS_PER_LAP = 1.5;

    private static final double HORIZONTAL_SHARE = 0.0;

    private static final double NO_COMPRESSION = 1.0;

    private static final int UNBOUNDED_LAP = 0;

    public record Resolved(WorldFold fold, double baseScale, double verticalShare, double factor) {
        boolean covers(WorldFold fold, double baseScale, double verticalShare) {
            return this.fold == fold && this.baseScale == baseScale && this.verticalShare == verticalShare;
        }
    }

    public static double resolve(ClimateCompressionCache cache, WorldFold fold, boolean climateField,
            DoubleList amplitudes, double lowestFreqInputFactor, double baseScale, double verticalShare) {
        Resolved resolved = cache.toroidal$climateCompression();
        if (resolved == null || !resolved.covers(fold, baseScale, verticalShare)) {
            resolved = new Resolved(fold, baseScale, verticalShare,
                    factor(fold, climateField, amplitudes, lowestFreqInputFactor, baseScale, verticalShare));
            cache.toroidal$climateCompression(resolved);
        }

        return resolved.factor();
    }

    public static double factor(WorldFold fold, boolean climateField, DoubleList amplitudes,
            double lowestFreqInputFactor, double baseScale, double verticalShare) {
        ClimateScale scale = fold.generationOptions().get(CompactBiomes.OPTION);
        if (scale.mode() == ClimateScale.Mode.OFF || verticalShare != HORIZONTAL_SHARE) {
            return NO_COMPRESSION;
        }

        if (scale.isFixed() && !climateField) {
            return NO_COMPRESSION;
        }

        int lap = lapBlocks(fold);
        if (lap == UNBOUNDED_LAP) {
            return NO_COMPRESSION;
        }

        if (scale.mode() == ClimateScale.Mode.CUSTOM) {
            return scale.factor();
        }

        double fitted = fittedFactor(amplitudes, lap * baseScale * lowestFreqInputFactor);
        return scale.mode() == ClimateScale.Mode.STRONG
                ? Math.max(fitted, ClimateScale.STRONG_FACTOR)
                : fitted;
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
