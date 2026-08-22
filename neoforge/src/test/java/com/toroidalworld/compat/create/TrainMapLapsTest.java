package com.toroidalworld.compat.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.toroidalworld.compat.create.TrainMapLaps.Range;
import com.toroidalworld.core.WrapDomain;

class TrainMapLapsTest {
    private static final int SURFACE_COPIES = 5;

    private static WrapDomain overworld() {
        return new WrapDomain(-256, 256);
    }

    private static int copyOf(int coord) {
        return Math.floorDiv(coord + 256, 512);
    }

    @Test
    void aViewInsideTheBoundsIsOneCopy() {
        Range range = TrainMapLaps.range(overworld(), -100, 200, SURFACE_COPIES);

        assertEquals(0, range.lowest());
        assertEquals(0, range.highest());
        assertFalse(range.capped());
    }

    @Test
    void aViewStraddlingTheSeamIsTwoCopies() {
        Range range = TrainMapLaps.range(overworld(), 246, 20, SURFACE_COPIES);

        assertEquals(2, range.kept());
        assertEquals(range.lowest() + 1, range.highest());
        assertFalse(range.capped());
    }

    @Test
    void aViewCoversEveryCopyItTouches() {
        int start = -1024;
        int span = 2048;
        Range range = TrainMapLaps.range(overworld(), start, span, SURFACE_COPIES);

        assertEquals(copyOf(start), range.lowest());
        assertEquals(copyOf(start + span - 1), range.highest());
        assertFalse(range.capped());
    }

    @Test
    void aViewEndingOnABoundDoesNotReachThePastTheEnd() {
        Range range = TrainMapLaps.range(overworld(), -256, 512, SURFACE_COPIES);

        assertEquals(0, range.lowest());
        assertEquals(0, range.highest());
    }

    @Test
    void aViewOfManyWorldsIsCappedRatherThanUnbounded() {
        int start = -20480;
        int span = 40960;
        Range range = TrainMapLaps.range(overworld(), start, span, SURFACE_COPIES);

        assertEquals(copyOf(start + span - 1) - copyOf(start) + 1, range.needed());
        assertEquals(2 * SURFACE_COPIES + 1, range.kept());
        assertTrue(range.capped());
    }

    @Test
    void theKeptCopiesSitAroundTheCanonicalOne() {
        Range range = TrainMapLaps.range(overworld(), -20480, 40960, SURFACE_COPIES);

        assertEquals(-SURFACE_COPIES, range.lowest());
        assertEquals(SURFACE_COPIES, range.highest());
    }

    @Test
    void anOffCentreViewIsCutAtTheCanonicalCopyNotAtItsOwnMiddle() {
        Range range = TrainMapLaps.range(overworld(), 200, 600, 1);

        assertEquals(3, range.needed());
        assertEquals(0, range.lowest());
        assertEquals(1, range.highest());
        assertTrue(range.capped());
    }

    @Test
    void aViewBeyondEveryDrawnCopyGetsNothing() {
        Range range = TrainMapLaps.range(overworld(), 5000, 100, 1);

        assertEquals(0, range.kept());
    }

    @Test
    void aSurfaceThatDoesNotRepeatGetsOneCopy() {
        Range range = TrainMapLaps.range(overworld(), 246, 20, 0);

        assertEquals(1, range.kept());
        assertTrue(range.capped());
    }

    @Test
    void aSurfaceRepeatingOnceEachSideGetsAtMostThree() {
        Range range = TrainMapLaps.range(overworld(), -20480, 40960, 1);

        assertEquals(3, range.kept());
        assertTrue(range.capped());
    }

    @Test
    void anUnboundedAxisIsOneCopy() {
        Range range = TrainMapLaps.range(new WrapDomain.Noop(), -1000, 5000, SURFACE_COPIES);

        assertEquals(1, range.kept());
        assertFalse(range.capped());
    }
}
