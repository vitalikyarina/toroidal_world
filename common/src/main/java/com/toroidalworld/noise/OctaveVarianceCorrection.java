package com.toroidalworld.noise;

import com.toroidalworld.core.WrapDomain;

public final class OctaveVarianceCorrection {
    private static final double[] CELLS_PER_LAP = {
            0.125, 0.1875, 0.25, 0.3125, 0.375, 0.4375, 0.5, 0.625, 0.75, 0.875, 1.0, 1.125, 1.25, 1.375, 1.4375
    };

    private static final double[] FLAT_CORRECTION = {
            0.138, 0.207, 0.275, 0.339, 0.395, 0.462, 0.519, 0.615, 0.696, 0.774, 0.814, 0.867, 0.897, 0.932, 0.945
    };

    private static final double[] ANCHOR_GAIN = {
            0.994, 0.972, 0.944, 0.956, 0.902, 0.871, 0.840, 0.779, 0.717, 0.643, 0.542, 0.471, 0.397, 0.364, 0.309
    };

    private static final double[] LIVENESS_CELLS_PER_LAP = {0.125, 0.25, 0.5, 0.75, 1.0, 1.25};

    private static final double[] LIVENESS_VERTICAL_CELLS = {0.0, 0.25, 0.5, 1.0, 2.0, 4.0, 8.0, 16.0};

    private static final double[][] LIVENESS = {
            {0.140, 0.246, 0.417, 0.617, 0.819, 0.903, 0.943, 0.936},
            {0.278, 0.346, 0.464, 0.686, 0.834, 0.911, 0.944, 0.943},
            {0.513, 0.542, 0.608, 0.743, 0.845, 0.931, 0.956, 0.961},
            {0.695, 0.746, 0.757, 0.858, 0.919, 0.947, 0.977, 0.973},
            {0.788, 0.838, 0.855, 0.899, 0.929, 0.978, 0.994, 0.992},
            {0.890, 0.910, 0.915, 0.943, 0.967, 0.975, 0.993, 0.990}};

    // The bound below which round(f) would fall under 2 — the regime where the sampler floors the period and the
    // extra lattice structure over-delivers amplitude.
    private static final double FLOORED_BOUND = 1.5;

    // The vertical span every ν is measured against. Dimensions differ (the nether is shorter), which shifts ν by a
    // constant factor; the liveness surface is smooth enough that the resulting k error stays under ~0.05.
    private static final double NOMINAL_HEIGHT_BLOCKS = 384.0;

    public static double factor(WrapDomain xDomain, WrapDomain zDomain, double scale, double verticalShare) {
        if (verticalShare < 0.0 || !xDomain.loops() || !zDomain.loops()) {
            return 1.0;
        }
        double xCells = xDomain.domainLength * scale;
        double zCells = zDomain.domainLength * scale;
        if (xCells >= FLOORED_BOUND || zCells >= FLOORED_BOUND) {
            return 1.0;
        }
        double cellsPerLap = (xCells + zCells) / 2.0;
        double damp = flat(cellsPerLap);
        double verticalCells = NOMINAL_HEIGHT_BLOCKS * verticalShare * scale;
        if (verticalCells > 0.0) {
            damp *= liveness(cellsPerLap, verticalCells) / liveness(cellsPerLap, 0.0);
        }
        return Math.min(damp, 1.0);
    }

    // Bilinear over the measured liveness grid, clamped to the edges on both axes: below the first row the relief of
    // the lowest measured f applies, past ν=16 the tail is flat, and the min-1 cap in factor keeps any extrapolated
    // relief from ever amplifying.
    static double liveness(double cellsPerLap, double verticalCells) {
        int row = upperIndex(LIVENESS_CELLS_PER_LAP, cellsPerLap);
        int column = upperIndex(LIVENESS_VERTICAL_CELLS, verticalCells);
        double rowBlend = blend(LIVENESS_CELLS_PER_LAP, row, cellsPerLap);
        double columnBlend = blend(LIVENESS_VERTICAL_CELLS, column, verticalCells);
        double atLowerRow = lerp(LIVENESS[row - 1][column - 1], LIVENESS[row - 1][column], columnBlend);
        double atUpperRow = lerp(LIVENESS[row][column - 1], LIVENESS[row][column], columnBlend);
        return lerp(atLowerRow, atUpperRow, rowBlend);
    }

    private static int upperIndex(double[] axis, double value) {
        for (int i = 1; i < axis.length; i++) {
            if (value <= axis[i]) {
                return i;
            }
        }
        return axis.length - 1;
    }

    private static double blend(double[] axis, int upper, double value) {
        double lower = axis[upper - 1];
        double span = axis[upper] - lower;
        double t = (value - lower) / span;
        return Math.max(0.0, Math.min(1.0, t));
    }

    private static double lerp(double from, double to, double t) {
        return from + t * (to - from);
    }

    // The anchor gain for a floored octave of a declared field, faded out as the octave's real vertical variation
    // grows — a live column carries its own DC through Y, and the liveness-calibrated damp already accounts for the
    // total variance there; by one vertical cell the anchor is gone. Zero when the damp does not apply.
    public static double anchorGain(WrapDomain xDomain, WrapDomain zDomain, double scale, double verticalShare) {
        if (verticalShare < 0.0 || !xDomain.loops() || !zDomain.loops()) {
            return 0.0;
        }
        double xCells = xDomain.domainLength * scale;
        double zCells = zDomain.domainLength * scale;
        if (xCells >= FLOORED_BOUND || zCells >= FLOORED_BOUND) {
            return 0.0;
        }
        double verticalFade = 1.0 - NOMINAL_HEIGHT_BLOCKS * verticalShare * scale;
        if (verticalFade <= 0.0) {
            return 0.0;
        }
        double cellsPerLap = (xCells + zCells) / 2.0;
        double gain = cellsPerLap <= CELLS_PER_LAP[0] ? ANCHOR_GAIN[0] : interpolate(ANCHOR_GAIN, cellsPerLap);
        return gain * verticalFade;
    }

    // Linear interpolation over the measured points. Below the first point the vanilla window is a near-linear patch
    // of one cell, so its std — and with it k — scales proportionally with the window size; past the last point the
    // period-1 regime only reaches f < 1.5, so the tail clamps.
    static double flat(double cellsPerLap) {
        if (cellsPerLap <= CELLS_PER_LAP[0]) {
            return FLAT_CORRECTION[0] * cellsPerLap / CELLS_PER_LAP[0];
        }
        return interpolate(FLAT_CORRECTION, cellsPerLap);
    }

    private static double interpolate(double[] values, double cellsPerLap) {
        for (int i = 1; i < CELLS_PER_LAP.length; i++) {
            if (cellsPerLap <= CELLS_PER_LAP[i]) {
                double t = (cellsPerLap - CELLS_PER_LAP[i - 1]) / (CELLS_PER_LAP[i] - CELLS_PER_LAP[i - 1]);
                return values[i - 1] + t * (values[i] - values[i - 1]);
            }
        }
        return values[values.length - 1];
    }

    private OctaveVarianceCorrection() {
    }
}
