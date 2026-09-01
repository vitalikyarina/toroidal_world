package com.toroidalworld.noise;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WrapDomain;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

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
            WorldFold transformer, double scale, double x, double z) {
        WrapDomain xDomain = transformer.blockDomain(Direction.Axis.X);
        WrapDomain zDomain = transformer.blockDomain(Direction.Axis.Z);
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

    static long skewDenominator(long xPeriod, long zPeriod) {
        if (xPeriod == UNBOUNDED_PERIOD) {
            return zPeriod;
        }

        if (zPeriod == UNBOUNDED_PERIOD) {
            return xPeriod;
        }

        return gcd(xPeriod, zPeriod);
    }

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

    private static double foldAndScale(WrapDomain domain, long period, double scale, double coord) {
        if (period == UNBOUNDED_PERIOD) {
            return coord * scale;
        }

        return domain.wrap(coord) * ((double) period / domain.domainLength);
    }

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

    private static double cornerNoise(int gradientIndex, double x, double z) {
        double falloff = KERNEL_RADIUS_SQR - x * x - z * z;
        if (falloff < 0.0) {
            return 0.0;
        }

        falloff *= falloff;
        int[] gradient = PeriodicNoiseSampler.GRADIENT[gradientIndex];
        return falloff * falloff * (gradient[0] * x + gradient[1] * z);
    }

    private static int p(int[] permutations, long index) {
        return permutations[(int) (index & PERMUTATION_MASK)];
    }

    private PeriodicSimplexSampler() {
    }
}
