package com.toroidalworld.compat.aeronautics;

import static com.toroidalworld.compat.CompatFoldFixture.PER_AXIS;
import static com.toroidalworld.compat.CompatFoldFixture.WORLD_BLOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import com.toroidalworld.core.WorldFold;
import com.toroidalworld.core.WorldFolds;

class MagnetSeamDeltaTest {
    private static final WorldFold UNWRAPPED = null;

    private static final double RISE = 6.0;
    private static final double HALF_WORLD = WORLD_BLOCKS / 2.0;

    private static Vector3d delta(double x) {
        return new Vector3d(x, RISE, 0.0);
    }

    @Test
    void aDeltaLongerThanHalfAWorldFoldsToTheShortOne() {
        assertEquals(delta(300.0 - WORLD_BLOCKS), MagnetSeamDelta.fold(PER_AXIS, delta(300.0)));
        assertEquals(delta(WORLD_BLOCKS - 300.0), MagnetSeamDelta.fold(PER_AXIS, delta(-300.0)));
    }

    @Test
    void aDeltaOfExactlyHalfAWorldStaysAsItIs() {
        Vector3d half = delta(HALF_WORLD);

        assertSame(half, MagnetSeamDelta.fold(PER_AXIS, half));
        assertEquals(delta(HALF_WORLD), half);
    }

    @Test
    void theCallerReadsTheFoldedDeltaBackOutOfTheVectorItPassed() {
        Vector3d relative = delta(300.0);

        assertSame(relative, MagnetSeamDelta.fold(PER_AXIS, relative));
        assertEquals(delta(300.0 - WORLD_BLOCKS), relative);
    }

    @Test
    void theFoldLeavesTheRiseAlone() {
        assertEquals(RISE, MagnetSeamDelta.fold(PER_AXIS, delta(300.0)).y);
    }

    @Test
    void aShortDeltaAndAnUnwrappedWorldLeaveTheVectorUntouched() {
        Vector3d shortDelta = delta(100.0);
        Vector3d across = delta(300.0);

        assertSame(shortDelta, MagnetSeamDelta.fold(PER_AXIS, shortDelta));
        assertEquals(delta(100.0), shortDelta);

        assertSame(across, MagnetSeamDelta.fold(UNWRAPPED, across));
        assertSame(across, MagnetSeamDelta.fold(WorldFolds.NOOP, across));
        assertEquals(delta(300.0), across);
    }
}
