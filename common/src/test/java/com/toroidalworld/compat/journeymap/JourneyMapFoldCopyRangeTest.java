package com.toroidalworld.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JourneyMapFoldCopyRangeTest {
    private static final int TORUS_CAP = 5;
    private static final int TORUS_COPIES = (2 * TORUS_CAP + 1) * (2 * TORUS_CAP + 1);
    private static final int CYLINDER_CAP = (TORUS_COPIES - 1) / 2;

    @Test
    void theCylinderCapSpendsTheTorusCopyBudgetOnOneAxis() {
        assertEquals(TORUS_CAP, JourneyMapFold.copyRangeCap(2), "the torus cap moved");
        assertEquals(CYLINDER_CAP, JourneyMapFold.copyRangeCap(1),
                "one looped axis does not get the (2 * 5 + 1)^2 - 1 copies split over two sides");
    }

    @Test
    void aViewportCoveredByThreeQuartersNeedsThatManyCopies() {
        assertEquals(3, JourneyMapFold.copyRange(1, 128.0, 512), "ceil(512 * 0.75 / 128) is 3");
        assertEquals(3, JourneyMapFold.copyRange(2, 128.0, 512), "the torus reads a different count under its cap");
    }

    @Test
    void theCapBindsPerLoopedAxisCount() {
        assertEquals(TORUS_CAP, JourneyMapFold.copyRange(2, 16.0, 1920), "the torus is not capped at 5");
        assertEquals(CYLINDER_CAP, JourneyMapFold.copyRange(1, 16.0, 1920), "the cylinder is not capped at 60");
        assertEquals(23, JourneyMapFold.copyRange(1, 64.0, 1920), "23 copies on a cylinder were capped");
    }

    @Test
    void anAxisWithNoPeriodDrawsNoCopies() {
        assertEquals(0, JourneyMapFold.copyRange(1, 0.0, 1920), "an unbounded axis got copies");
    }
}
