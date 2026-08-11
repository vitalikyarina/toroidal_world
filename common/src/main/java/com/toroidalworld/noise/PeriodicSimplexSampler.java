package com.toroidalworld.noise;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;

import net.minecraft.util.Mth;

// The periodic replacement for SimplexNoise's 2D field — the one vanilla noise family PeriodicNoiseSampler cannot
// serve, because simplex reads its gradients off a SKEWED lattice rather than the axis-aligned one Perlin walks.
// Biome's three climate noises are its only vanilla users, and everything temperature-derived reads through them:
// where a frozen ocean freezes, where snow settles, whether the sky drops rain or snow. Left unwrapped that field has
// a hard edge exactly at the seam, and no fold can move it — both sides of the seam are in bounds, so folding them is
// the identity.
//
// WHAT CLOSES IT. A sample skews before flooring: u = (1+F)x + F z, v = F x + (1+F) z, and the lattice cell is
// (floor u, floor v). Advancing x by one world width leaves the sample identical only if it moves (u, v) by a whole
// number of cells — that is, only if (1+F)W and F·W are both integers, hence W and F·W both integers. Vanilla's
// F2 = (√3−1)/2 is irrational, so NO width satisfies that: the tear is a property of the lattice, not a fold someone
// forgot. The skew is therefore rationalised against the period, F = floor(F2 · q) / q — floored for the reason
// skewNumerator gives — and the unskew follows it as G = F / (1 + 2F), which is the exact inverse of that skew and
// keeps the lattice a valid, slightly flattened, triangular one. Everything else is vanilla's algorithm verbatim: the
// same permutation table, the same gradients, the same corner kernel, the same 70x normalisation.
//
// WHAT A LAP DOES TO THE CELL INDEX. One lap along X moves (u, v) by (period_x + b_x, b_x), one along Z by
// (b_z, period_z + b_z), where b is F times that axis' period. Those two integer vectors span the lattice L of cell
// indices naming the same ground, so a cell is reduced modulo L before the permutation cascade — the general form of
// what PeriodicNoiseSampler does per axis with a plain modulo. The three corners reduce independently, so the last
// cell of a lap interpolates into the first: that interpolation is the seam.
//
// THE SHARED DENOMINATOR. F must come out a whole number of cells per lap on BOTH axes, so its denominator divides
// gcd(period_x, period_z). Every world this mod can create is square — the settings screen takes one size — so that
// gcd is the period itself and the rationalisation moves the skew by well under a percent. Hand-edited bounds with
// near-coprime periods degrade toward F = 0, which is gradient noise on a square lattice: a different texture, still
// seamless, which is the trade the End islands already make for their own layout.
//
// Bit-exact vanilla parity has no regime here, unlike the Perlin sampler's: a rationalised skew is never vanilla's
// skew. That is inherent — continuity across the seam is incompatible with an irrational one — so an unwrapped level
// is kept vanilla by the caller's guard, not by a parity window inside the sampler.
public final class PeriodicSimplexSampler {
    private static final long UNBOUNDED_PERIOD = 0L;

    private static final double SQRT_3 = Math.sqrt(3.0);
    static final double VANILLA_SKEW = 0.5 * (SQRT_3 - 1.0);
    private static final double VANILLA_UNSKEW = (3.0 - SQRT_3) / 6.0;

    private static final double KERNEL_RADIUS_SQR = 0.5;
    private static final double NORMALIZATION = 70.0;
    private static final int GRADIENT_COUNT = 12;
    private static final int PERMUTATION_MASK = 0xFF;

    public static double sample(int[] permutations, double xOffset, double zOffset,
            WorldLoopTransformer transformer, double scale, double x, double z) {
        WrapDomain xDomain = transformer.coords.x;
        WrapDomain zDomain = transformer.coords.z;
        long xPeriod = PeriodicNoiseSampler.period(xDomain, scale);
        long zPeriod = PeriodicNoiseSampler.period(zDomain, scale);
        long denominator = skewDenominator(xPeriod, zPeriod);
        long numerator = skewNumerator(denominator);

        double skew;
        double unskew;
        if (denominator == UNBOUNDED_PERIOD) {
            skew = VANILLA_SKEW;
            unskew = VANILLA_UNSKEW;
        } else {
            skew = (double) numerator / denominator;
            unskew = skew / (1.0 + 2.0 * skew);
        }

        // The lap vectors, in cell units. An unbounded axis has period 0 and so contributes the zero vector, which the
        // reduction reads as "this axis names no repeats".
        long xLapV = denominator == UNBOUNDED_PERIOD ? 0L : numerator * (xPeriod / denominator);
        long zLapU = denominator == UNBOUNDED_PERIOD ? 0L : numerator * (zPeriod / denominator);
        long xLapU = xPeriod + xLapV;
        long zLapV = zPeriod + zLapU;

        double xs = foldAndScale(xDomain, xPeriod, scale, x) + xOffset;
        double zs = foldAndScale(zDomain, zPeriod, scale, z) + zOffset;

        double skewed = (xs + zs) * skew;
        long uCell = Mth.lfloor(xs + skewed);
        long vCell = Mth.lfloor(zs + skewed);
        double unskewed = (uCell + vCell) * unskew;
        double xFrac = xs - (uCell - unskewed);
        double zFrac = zs - (vCell - unskewed);

        long uStep;
        long vStep;
        if (xFrac > zFrac) {
            uStep = 1L;
            vStep = 0L;
        } else {
            uStep = 0L;
            vStep = 1L;
        }

        double xMid = xFrac - uStep + unskew;
        double zMid = zFrac - vStep + unskew;
        double xFar = xFrac - 1.0 + 2.0 * unskew;
        double zFar = zFrac - 1.0 + 2.0 * unskew;

        double near = cornerNoise(
                gradient(permutations, uCell, vCell, xLapU, xLapV, zLapU, zLapV), xFrac, zFrac);
        double mid = cornerNoise(
                gradient(permutations, uCell + uStep, vCell + vStep, xLapU, xLapV, zLapU, zLapV), xMid, zMid);
        double far = cornerNoise(
                gradient(permutations, uCell + 1L, vCell + 1L, xLapU, xLapV, zLapU, zLapV), xFar, zFar);

        return NORMALIZATION * (near + mid + far);
    }

    // Cells per lap for one axis, shared with the Perlin sampler so both fields quantize a scale the same way. The
    // simplex skew is then rationalised against these, which is why they have to be the same integers.
    static long skewDenominator(long xPeriod, long zPeriod) {
        if (xPeriod == UNBOUNDED_PERIOD) {
            return zPeriod;
        }

        if (zPeriod == UNBOUNDED_PERIOD) {
            return xPeriod;
        }

        return gcd(xPeriod, zPeriod);
    }

    // Floored, never rounded to nearest, and that is a correctness rule rather than a taste. Vanilla's kernel radius
    // of 0.5 is EXACTLY tight: the nearest lattice corner the simplex leaves out of a cell's three sits at squared
    // distance (1 - 2G)² / ((1-G)² + G²), which comes to exactly 0.5 at vanilla's unskew and falls BELOW it as soon as
    // the skew is rounded up. A corner inside the kernel that one neighbouring cell counts and the next does not is a
    // step in the field — small (about 4e-6 at a 5% overshoot) but present at every cell edge in the world. Flooring
    // keeps the skew under vanilla's, where that distance stays above 0.5 and the kernel stays exactly tight, so the
    // field is continuous with no tolerance attached and vanilla's own amplitude. The price is paid in the lattice's
    // shape instead: the triangles run up to 1/q flatter than equilateral, which is anisotropy, not a tear.
    static long skewNumerator(long denominator) {
        return denominator == UNBOUNDED_PERIOD ? 0L : (long) Math.floor(VANILLA_SKEW * denominator);
    }

    private static long gcd(long first, long second) {
        long larger = first;
        long smaller = second;
        while (smaller != 0L) {
            long remainder = larger % smaller;
            larger = smaller;
            smaller = remainder;
        }

        return larger;
    }

    // A looped axis folds into its bounds first — fold(x) == fold(x + width) bit-exactly, which is what makes a lap
    // exact rather than epsilon-close — and is then scaled by period / width, the quantized scale that advances the
    // field by exactly one period per lap. An unbounded axis is vanilla's straight line; simplex has no equivalent of
    // PerlinNoise.wrap, so nothing is folded off a far-out coordinate that vanilla would have kept.
    private static double foldAndScale(WrapDomain domain, long period, double scale, double coord) {
        if (period == UNBOUNDED_PERIOD) {
            return coord * scale;
        }

        return domain.wrap(coord) * ((double) period / domain.domainLength);
    }

    // Vanilla's gradient cascade with the cell index first brought into one fundamental domain of the lap lattice, so
    // that every cell naming the same ground hashes to the same gradient. Solving for the lap counts in exact integer
    // arithmetic is what makes the residue canonical: shifting the cell by a lap vector shifts one numerator by
    // exactly the determinant and leaves the other alone, so the floor moves by exactly one and the residue does not
    // move at all.
    private static int gradient(int[] permutations, long u, long v,
            long xLapU, long xLapV, long zLapU, long zLapV) {
        long determinant = xLapU * zLapV - zLapU * xLapV;
        long reducedU = u;
        long reducedV = v;

        if (determinant != 0L) {
            long xLaps = Math.floorDiv(zLapV * u - zLapU * v, determinant);
            long zLaps = Math.floorDiv(xLapU * v - xLapV * u, determinant);
            reducedU = u - xLaps * xLapU - zLaps * zLapU;
            reducedV = v - xLaps * xLapV - zLaps * zLapV;
        } else if (xLapU != 0L) {
            long laps = Math.floorDiv(xLapU * u + xLapV * v, xLapU * xLapU + xLapV * xLapV);
            reducedU = u - laps * xLapU;
            reducedV = v - laps * xLapV;
        } else if (zLapV != 0L) {
            long laps = Math.floorDiv(zLapU * u + zLapV * v, zLapU * zLapU + zLapV * zLapV);
            reducedU = u - laps * zLapU;
            reducedV = v - laps * zLapV;
        }

        return p(permutations, reducedU + p(permutations, reducedV)) % GRADIENT_COUNT;
    }

    // Vanilla SimplexNoise.getCornerNoise3D with the third axis dropped: the 2D call passes z = 0, so the kernel loses
    // one square and the dot product one term. The gradient's Y component is the one that carries this axis — vanilla
    // hands its second 2D coordinate in as y.
    private static double cornerNoise(int gradientIndex, double x, double z) {
        double falloff = KERNEL_RADIUS_SQR - x * x - z * z;
        if (falloff < 0.0) {
            return 0.0;
        }

        falloff *= falloff;
        int[] gradient = PeriodicNoiseSampler.GRADIENT[gradientIndex];
        return falloff * falloff * (gradient[0] * x + gradient[1] * z);
    }

    // Vanilla SimplexNoise.p over a long index: only the low 8 bits reach the table, so a residue from a giant lattice
    // loses nothing the narrowing cast could have kept.
    private static int p(int[] permutations, long index) {
        return permutations[(int) (index & PERMUTATION_MASK)];
    }

    private PeriodicSimplexSampler() {
    }
}
