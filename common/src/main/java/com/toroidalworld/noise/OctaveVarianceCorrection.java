package com.toroidalworld.noise;

import com.toroidalworld.core.WrapDomain;

// On a torus an octave whose true cells-per-lap f = width·scale falls under 1.5 is floored to a four-cell lattice
// (LapFloor.FOUR_CELLS; a single-cell lap collapses to one smoothstep-warped plane, which no amplitude policy
// can turn back into noise; four cells rather than the minimal two keeps the floored wavelength at 128 blocks on a
// 512-block world instead of the axis-aligned 256-block lattice that read as square mountains in-game). The floor
// buys structure at the cost of amplitude: the four-cell fold shows near-full noise where vanilla's window sees only
// a fraction of a near-constant cell, over-delivering rms up to ×7.7 at the lowest octaves. This damps the floored
// octaves back onto the vanilla window rms while keeping the closed lap (and so the seam) intact. Damp-only by
// construction: the four-cell fold's rms (0.2664) exceeds the vanilla window rms at every f < 1.5, so the table
// never exceeds 1.
//
// Everything here is calibrated, not derived (FieldDistributionProbeTest, single real vanilla octaves, 512-block
// laps): the flat table is sqrt of the mean-variance ratio, vanilla window over the floored fold, per f (2048
// seeds). A live axis — the second horizontal one (rectangular calibration) or Y (vertical-liveness calibration) —
// shrinks the deficit, because real variation along it dominates the spread on both sides of the comparison. Hence
// the correction applies only when BOTH horizontal axes are in the floored regime and the field is vertically flat
// (the caller declares flatness through GenerationTransformerContext), at the arithmetic-mean f of the two axes.
// A cylinder never floors: LapFloor.HELD keeps a starved octave constant along the ring, and the strip rms then sits
// within 2% of vanilla's (the cylinder calibration), so the identity on an unbounded axis is measured, not deferred.
// Known deferred residuals, measured: exactly one floored axis over-delivers up to ×1.21 (rectangular worlds do not
// exist yet — calibrate before shipping per-axis sizes), and vertically-live fields with floored octaves
// over-deliver up to ×1.7 at half a vertical cell per world height (the 3D cave family's slow modulators).
public final class OctaveVarianceCorrection {
    private static final double[] CELLS_PER_LAP = {
            0.125, 0.1875, 0.25, 0.3125, 0.375, 0.4375, 0.5, 0.625, 0.75, 0.875, 1.0, 1.125, 1.25, 1.375, 1.4375
    };

    private static final double[] FLAT_CORRECTION = {
            0.130, 0.194, 0.258, 0.319, 0.371, 0.435, 0.488, 0.578, 0.654, 0.727, 0.765, 0.815, 0.843, 0.876, 0.888
    };

    // The DC component: vanilla's window mean wanders seed to seed (mean-spread 0.271 at f=0.125 down to 0.132 at
    // 1.4375), while the damped fold's contribution to the world mean collapses to k·0.054 — without restoration
    // every toroidal world parks at the spline's coast band (measured in-game as all-coast worlds). The gain scales a
    // fixed-lattice-point sample of the same octave (pointwise spread 0.2763), sized so the combined mean spread
    // lands on vanilla's: a(f) = sqrt(vanillaMeanSpread² − (k·foldMeanSpread)²) / anchorSpread.
    private static final double[] ANCHOR_GAIN = {
            0.979, 0.959, 0.934, 0.948, 0.898, 0.872, 0.846, 0.795, 0.746, 0.687, 0.602, 0.551, 0.498, 0.481, 0.445
    };

    // The liveness grid: measured vanilla-over-floored rms ratio for an octave sampled across ν vertical cells per
    // world height (rows = cells per lap, columns = LIVENESS_VERTICAL_CELLS). Real vertical variation restores the
    // spread on both sides of the comparison, so the deficit shrinks as ν grows; the correction applies the row-wise
    // ratio against the ν=0 column as a relief on top of the finer flat table.
    private static final double[] LIVENESS_CELLS_PER_LAP = {0.125, 0.25, 0.5, 0.75, 1.0, 1.25};

    private static final double[] LIVENESS_VERTICAL_CELLS = {0.0, 0.25, 0.5, 1.0, 2.0, 4.0, 8.0, 16.0};

    private static final double[][] LIVENESS = {
            {0.129, 0.229, 0.393, 0.604, 0.813, 0.901, 0.942, 0.928},
            {0.264, 0.328, 0.446, 0.668, 0.829, 0.911, 0.944, 0.939},
            {0.484, 0.508, 0.586, 0.726, 0.839, 0.927, 0.960, 0.951},
            {0.655, 0.698, 0.730, 0.833, 0.910, 0.942, 0.974, 0.969},
            {0.755, 0.792, 0.825, 0.876, 0.934, 0.979, 0.988, 0.985},
            {0.824, 0.860, 0.883, 0.927, 0.957, 0.974, 0.990, 0.980}};

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
