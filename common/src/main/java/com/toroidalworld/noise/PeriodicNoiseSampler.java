package com.toroidalworld.noise;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WrapDomain;
import com.toroidalworld.noise.GenerationTransformerContext.Context;

import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

public final class PeriodicNoiseSampler {
    static final int[][] GRADIENT = {
            {1, 1, 0},
            {-1, 1, 0},
            {1, -1, 0},
            {-1, -1, 0},
            {1, 0, 1},
            {-1, 0, 1},
            {1, 0, -1},
            {-1, 0, -1},
            {0, 1, 1},
            {0, -1, 1},
            {0, 1, -1},
            {0, -1, -1},
            {1, 1, 0},
            {0, -1, 1},
            {-1, 1, 0},
            {0, -1, -1}
    };

    private static final long UNBOUNDED_PERIOD = 0L;

    private static final long FLOORED_PERIOD = 4L;

    public static double sample(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldFold transformer, Context context,
            double x, double y, double z, double yScale, double yFudge) {
        SlotAxes axes = context.slotAxes();
        double scale = context.horizontalScale();

        long xPeriod;
        long yPeriod;
        long zPeriod;
        double xs;
        double ys;
        double zs;
        double correction = 1.0;
        double anchor = 0.0;
        if (axes == SlotAxes.DEFAULT && context.xDivisor() == 1.0 && context.zDivisor() == 1.0) {
            WrapDomain xDomain = transformer.blockDomain(Direction.Axis.X);
            WrapDomain zDomain = transformer.blockDomain(Direction.Axis.Z);
            xPeriod = period(xDomain, scale);
            yPeriod = UNBOUNDED_PERIOD;
            zPeriod = period(zDomain, scale);
            xs = foldAndScale(xDomain, xPeriod, scale, x) + xOffset;
            ys = y + yOffset;
            zs = foldAndScale(zDomain, zPeriod, scale, z) + zOffset;

            // The variance correction is a per-octave constant, so scaling the sample scales the whole field
            // uniformly. verticalShare is the caller's vertical-to-horizontal scale ratio (negative = undeclared,
            // correction off); the deeper the field really varies with Y, the less damping its floored octaves need
            // (see the correction class), and most octaves fold to periods above the floored bound, where the factor
            // is 1.
            double verticalShare = context.verticalShare();
            correction = OctaveVarianceCorrection.factor(xDomain, zDomain, xPeriod, zPeriod, scale, verticalShare);

            // The DC restoration: a fixed-lattice-point sample of the same octave — constant across the whole world,
            // Y included, so it shifts the field without adding any variance the damp calibration already accounts
            // for. A constant cannot open the seam, and for the flat router fields it is what spreads toroidal worlds
            // across vanilla's ocean-to-inland range instead of parking every one at the coast band.
            double anchorGain = OctaveVarianceCorrection.anchorGain(xDomain, zDomain, xPeriod, zPeriod, scale,
                    verticalShare);
            if (anchorGain > 0.0) {
                anchor = anchorGain * anchorSample(permutations, xDomain, zDomain, xPeriod, zPeriod, scale,
                        xOffset, yOffset, zOffset);
            }
        } else {
            WrapDomain xDomain = axes.x().domainOf(transformer);
            WrapDomain yDomain = axes.y().domainOf(transformer);
            WrapDomain zDomain = axes.z().domainOf(transformer);
            double xSlotScale = scale / axes.x().divisorIn(context);
            double ySlotScale = scale / axes.y().divisorIn(context);
            double zSlotScale = scale / axes.z().divisorIn(context);
            xPeriod = period(xDomain, xSlotScale);
            yPeriod = period(yDomain, ySlotScale);
            zPeriod = period(zDomain, zSlotScale);
            xs = slotCoord(axes.x(), xDomain, xPeriod, xSlotScale, x) + xOffset;
            ys = slotCoord(axes.y(), yDomain, yPeriod, ySlotScale, y) + yOffset;
            zs = slotCoord(axes.z(), zDomain, zPeriod, zSlotScale, z) + zOffset;
        }

        int xCell = Mth.floor(xs);
        int yCell = Mth.floor(ys);
        int zCell = Mth.floor(zs);
        double xFrac = xs - xCell;
        double yFrac = ys - yCell;
        double zFrac = zs - zCell;

        double yFracFudge;
        if (yScale != 0.0) {
            double fudgeLimit = yFudge >= 0.0 && yFudge < yFrac ? yFudge : yFrac;
            yFracFudge = Mth.floor(fudgeLimit / yScale + 1.0E-7F) * yScale;
        } else {
            yFracFudge = 0.0;
        }

        return correction * sampleAndLerp(permutations, xCell, yCell, zCell, xFrac, yFrac - yFracFudge, zFrac,
                yFrac, xPeriod, yPeriod, zPeriod) + anchor;
    }

    private static double anchorSample(byte[] permutations, WrapDomain xDomain, WrapDomain zDomain,
            long xPeriod, long zPeriod, double scale, double xOffset, double yOffset, double zOffset) {
        double xs = foldAndScale(xDomain, xPeriod, scale, 0.0) + xOffset;
        double zs = foldAndScale(zDomain, zPeriod, scale, 0.0) + zOffset;
        int xCell = Mth.floor(xs);
        int zCell = Mth.floor(zs);
        int yCell = Mth.floor(yOffset);
        double yFrac = yOffset - yCell;
        return sampleAndLerp(permutations, xCell, yCell, zCell, xs - xCell, yFrac, zs - zCell, yFrac,
                xPeriod, UNBOUNDED_PERIOD, zPeriod);
    }

    // A slot carrying no world axis arrives already scaled by its caller, so scaling it again would move the lattice.
    private static double slotCoord(SlotAxis axis, WrapDomain domain, long period, double scale, double coord) {
        if (!axis.carriesWorldAxis()) {
            return coord;
        }

        return foldAndScale(domain, period, scale, coord);
    }

    // An octave whose rounding falls under 2 is degenerate: at period 1 every cell index wraps to 0, all corners
    // hash to the same gradient, and the octave collapses to a single smoothstep-warped plane spanning the world —
    // a monotone ramp no amplitude correction can turn back into noise. Those octaves are floored to 4 cells per lap
    // rather than the minimal 2: a 2-cell closed walk is an axis-aligned lattice whose 256-block wavelength (on a
    // 512-block world) reads as square mountains in-game, while 4 cells halves the wavelength to 128 blocks and
    // blurs the axis alignment. Octaves whose natural rounding reaches 2 already match vanilla's window and keep it;
    // the amplitude the floored structure over-delivers is damped back by OctaveVarianceCorrection.
    static long period(WrapDomain domain, double scale) {
        if (domain instanceof WrapDomain.Noop) {
            return UNBOUNDED_PERIOD;
        }

        long rounded = Math.round(domain.domainLength * scale);
        return rounded < 2L ? FLOORED_PERIOD : rounded;
    }

    static double foldAndScale(WrapDomain domain, long period, double scale, double coord) {
        if (period == UNBOUNDED_PERIOD) {
            return PerlinNoise.wrap(coord * scale);
        }

        return domain.wrap(coord) * ((double) period / domain.domainLength);
    }

    private static double sampleAndLerp(byte[] permutations, int xCell, int yCell, int zCell,
            double xFrac, double yFracFudged, double zFrac, double yFracOriginal,
            long xPeriod, long yPeriod, long zPeriod) {
        int x0 = p(permutations, wrapCell(xCell, xPeriod));
        int x1 = p(permutations, wrapCell(xCell + 1L, xPeriod));
        long y0 = wrapCell(yCell, yPeriod);
        long y1 = wrapCell(yCell + 1L, yPeriod);
        int xy00 = p(permutations, x0 + y0);
        int xy01 = p(permutations, x0 + y1);
        int xy10 = p(permutations, x1 + y0);
        int xy11 = p(permutations, x1 + y1);
        long z0 = wrapCell(zCell, zPeriod);
        long z1 = wrapCell(zCell + 1L, zPeriod);
        double d000 = gradDot(p(permutations, xy00 + z0), xFrac, yFracFudged, zFrac);
        double d100 = gradDot(p(permutations, xy10 + z0), xFrac - 1.0, yFracFudged, zFrac);
        double d010 = gradDot(p(permutations, xy01 + z0), xFrac, yFracFudged - 1.0, zFrac);
        double d110 = gradDot(p(permutations, xy11 + z0), xFrac - 1.0, yFracFudged - 1.0, zFrac);
        double d001 = gradDot(p(permutations, xy00 + z1), xFrac, yFracFudged, zFrac - 1.0);
        double d101 = gradDot(p(permutations, xy10 + z1), xFrac - 1.0, yFracFudged, zFrac - 1.0);
        double d011 = gradDot(p(permutations, xy01 + z1), xFrac, yFracFudged - 1.0, zFrac - 1.0);
        double d111 = gradDot(p(permutations, xy11 + z1), xFrac - 1.0, yFracFudged - 1.0, zFrac - 1.0);
        double xAlpha = Mth.smoothstep(xFrac);
        double yAlpha = Mth.smoothstep(yFracOriginal);
        double zAlpha = Mth.smoothstep(zFrac);
        return Mth.lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
    }

    private static long wrapCell(long cell, long period) {
        return period == UNBOUNDED_PERIOD ? cell : Math.floorMod(cell, period);
    }

    private static int p(byte[] permutations, long index) {
        return permutations[(int) (index & 0xFFL)] & 0xFF;
    }

    private static double gradDot(int hash, double x, double y, double z) {
        int[] gradient = GRADIENT[hash & 15];
        return gradient[0] * x + gradient[1] * y + gradient[2] * z;
    }

    private PeriodicNoiseSampler() {
    }
}
