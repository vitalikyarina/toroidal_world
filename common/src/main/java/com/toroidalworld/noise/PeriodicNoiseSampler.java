package com.toroidalworld.noise;

import com.toroidalworld.core.WorldLoopTransformer;
import com.toroidalworld.core.WrapDomain;

import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

// The periodic replacement for ImprovedNoise: vanilla's own lattice made to close on itself. Vanilla Perlin is already
// periodic — p(x) reads the 256-entry permutation table through x & 0xFF, so the field repeats every 256 cells per
// axis — and the only change here is the period constant: the lattice cell index of a looped axis is wrapped by the
// loop period before the permutation cascade, so cell P-1 interpolates into cell 0 through vanilla's own Mth.lerp3 and
// the seam closes exactly. Everything else — the gradients, the smoothstep, the yScale/yFudge quantization, Y as a
// real axis — is vanilla's algorithm verbatim.
//
// The block coordinate is folded into the world bounds before scaling (fold-first), so two coordinates one world width
// apart become the same double before any arithmetic — one-lap closure is bit-exact by construction, not epsilon-close.
//
// The callers hand X/Z in raw block coordinates and park their total per-octave scale in the generation context
// (PerlinNoiseMixin, BlendedNoiseMixin, SurfaceSystemMixin, the DensityFunctions mixins). That scale becomes the
// lattice period: P = round(width × scale) cells per lap, and the coordinate is scaled by the quantized P / width
// rather than the scale itself so a lap always advances the field by exactly P cells. With power-of-two widths and
// scales P is exact and P / width == scale; and whenever 256 divides P the wrap below is a no-op — the output is
// bit-for-bit vanilla, which on a power-of-two world covers every octave except the lowest-frequency ones.
public final class PeriodicNoiseSampler {
    // Copy of SimplexNoise.GRADIENT (protected there): the 12 cube-edge vectors plus 4 repeats vanilla Perlin hashes
    // into. Copied rather than access-widened so the sampler stays callable from plain unit tests. Shared with
    // PeriodicSimplexSampler, which hashes into the same table — vanilla's simplex and Perlin both read this one.
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

    public static double sample(byte[] permutations, double xOffset, double yOffset, double zOffset,
            WorldLoopTransformer transformer, double scale,
            double x, double y, double z, double yScale, double yFudge) {
        WrapDomain xDomain = transformer.coords.x;
        WrapDomain zDomain = transformer.coords.z;
        long xPeriod = period(xDomain, scale);
        long zPeriod = period(zDomain, scale);

        double xs = foldAndScale(xDomain, xPeriod, scale, x) + xOffset;
        double ys = y + yOffset;
        double zs = foldAndScale(zDomain, zPeriod, scale, z) + zOffset;
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

        return sampleAndLerp(permutations, xCell, yCell, zCell, xFrac, yFrac - yFracFudge, zFrac, yFrac,
                xPeriod, zPeriod);
    }

    // Cells per lap for one axis: the world width at this scale, rounded to the integer the lattice needs. The floor
    // of 1 covers octaves whose single cell outgrows the whole world — the octave degrades to one repeated cell,
    // continuous at the seam, in the same regime where the old circle embedding collapsed to near-constant.
    static long period(WrapDomain domain, double scale) {
        if (domain instanceof WrapDomain.Noop) {
            return UNBOUNDED_PERIOD;
        }

        return Math.max(1L, Math.round(domain.domainLength * scale));
    }

    // A looped axis folds into its bounds first — fold(x) == fold(x + width) bit-exactly, which is the whole closure
    // argument — and is then scaled by period / width, the quantized scale that advances exactly one period per lap.
    // An unbounded axis is vanilla's straight line: coordinate times scale, folded by PerlinNoise.wrap the same way
    // PerlinNoise.getValue would have before the caller mixins started handing coordinates over raw.
    static double foldAndScale(WrapDomain domain, long period, double scale, double coord) {
        if (period == UNBOUNDED_PERIOD) {
            return PerlinNoise.wrap(coord * scale);
        }

        return domain.wrap(coord) * ((double) period / domain.domainLength);
    }

    // Vanilla ImprovedNoise.sampleAndLerp with one change: the X/Z cell index passes through wrapCell before the
    // permutation cascade. Both corners wrap independently, so the last cell's right corner is the first cell — that
    // interpolation is the seam.
    private static double sampleAndLerp(byte[] permutations, int xCell, int yCell, int zCell,
            double xFrac, double yFracFudged, double zFrac, double yFracOriginal, long xPeriod, long zPeriod) {
        int x0 = p(permutations, wrapCell(xCell, xPeriod));
        int x1 = p(permutations, wrapCell(xCell + 1L, xPeriod));
        int xy00 = p(permutations, x0 + yCell);
        int xy01 = p(permutations, x0 + yCell + 1);
        int xy10 = p(permutations, x1 + yCell);
        int xy11 = p(permutations, x1 + yCell + 1);
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

    // Vanilla ImprovedNoise.p over a long index: only the low 8 bits reach the table, so the narrowing cast loses
    // nothing a giant period could have put above them.
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
