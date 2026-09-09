package com.toroidalworld.engine.noise;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.toroidalworld.core.FlatShape;
import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;
import com.toroidalworld.core.WorldLoopBounds;
import com.toroidalworld.core.WrapDomain;

import net.minecraft.core.Direction;

class DomainWarpTest {
    private static final int CHUNK_WIDTH = 32;

    private static final double XZ_SCALE = 0.25;

    private static final double SHIFT = 3.0;

    private static final int BLOCK = 137;

    private static final double[] FACTORS = {1.0, 2.0, 6.24};

    private static final WorldFold FOLD = WorldFolds.of(FlatShape.torus(WorldLoopBounds.ofWidth(CHUNK_WIDTH)));

    private static final WrapDomain DOMAIN = FOLD.blockDomain(Direction.Axis.X);

    @Test
    void warpDisplacementIsTheSameCellCountAtEveryCompressionFactor() {
        for (double factor : FACTORS) {
            double scale = XZ_SCALE * factor;
            long period = PeriodicNoiseSampler.period(DOMAIN, scale, LapFloor.of(FOLD));

            assertEquals(SHIFT, cells(SHIFT, scale, period) - cells(0.0, scale, period), SHIFT / period,
                    "compression factor " + factor);
        }
    }

    @Test
    void theWarpedCoordinateStillClosesOnALap() {
        for (double factor : FACTORS) {
            double scale = XZ_SCALE * factor;

            assertEquals(DomainWarp.apply(DOMAIN, BLOCK, SHIFT, scale),
                    DomainWarp.apply(DOMAIN, BLOCK + DOMAIN.domainLength, SHIFT, scale),
                    "compression factor " + factor);
        }
    }

    private static double cells(double shift, double scale, long period) {
        return PeriodicNoiseSampler.foldAndScale(DOMAIN, period, scale,
                DomainWarp.apply(DOMAIN, BLOCK, shift, scale));
    }
}
