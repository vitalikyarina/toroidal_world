package com.toroidalworld.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class JourneyMapFoldCopyRangeTest {
    private static final int BLIT_BUDGET = 16384;

    @Test
    void theCapSpendsTheBlitBudgetOverTheTilesWithContent() {
        assertEquals(63, JourneyMapFold.copyRangeCap(2, 1), "one tile on a torus: (sqrt(16384) - 1) / 2 = 63");
        assertEquals(8191, JourneyMapFold.copyRangeCap(1, 1), "one tile on a cylinder: (16384 - 1) / 2 = 8191");
        assertEquals(15, JourneyMapFold.copyRangeCap(2, 16), "16 tiles on a torus: (sqrt(1024) - 1) / 2 = 15");
        assertEquals(511, JourneyMapFold.copyRangeCap(1, 16), "16 tiles on a cylinder: (1024 - 1) / 2 = 511");
        assertEquals(0, JourneyMapFold.copyRangeCap(2, BLIT_BUDGET / 4), "4 blits per tile leave no torus copy");
        assertEquals(1, JourneyMapFold.copyRangeCap(1, BLIT_BUDGET / 4), "4 blits per tile leave one cylinder copy per side");
        assertEquals(0, JourneyMapFold.copyRangeCap(0, 1), "no looped axis got copies");
    }

    @Test
    void aViewportCoveredByThreeQuartersNeedsThatManyCopies() {
        assertEquals(3, JourneyMapFold.copyRange(1, 1, 128.0, 512), "ceil(512 * 0.75 / 128) is 3");
        assertEquals(3, JourneyMapFold.copyRange(2, 1, 128.0, 512), "the torus reads a different count under its cap");
    }

    @Test
    void theCapBindsWhenTheViewportAsksForMore() {
        assertEquals(63, JourneyMapFold.copyRange(2, 1, 16.0, 1920), "ceil(1920 * 0.75 / 16) = 90 was not capped at 63");
        assertEquals(90, JourneyMapFold.copyRange(1, 1, 16.0, 1920), "90 copies on a one-tile cylinder were capped");
        assertEquals(15, JourneyMapFold.copyRange(2, 16, 16.0, 1920), "90 copies over 16 torus tiles were not capped at 15");
    }

    @Test
    void anAxisWithNoPeriodDrawsNoCopies() {
        assertEquals(0, JourneyMapFold.copyRange(1, 1, 0.0, 1920), "an unbounded axis got copies");
    }

    @Test
    void anUnboundedAxisCountsTheTilesAcrossTheViewport() {
        assertEquals(6, JourneyMapFold.viewportTiles(256, 1280), "ceil(1280 / 256) + 1 is 6");
        assertEquals(161, JourneyMapFold.viewportTiles(8, 1280), "ceil(1280 / 8) + 1 is 161");
        assertEquals(1, JourneyMapFold.viewportTiles(0, 1280), "a zero zoom is not one tile");
    }

    @Test
    void theViewSpanIsTheWindowInBlocksAroundTheCenter() {
        assertArrayEquals(new int[] {-640, 640}, JourneyMapFold.viewSpan(0.0, 1280, 512),
                "1280 px at 512 px per 512-block region is 1280 blocks, 640 each side");
        assertArrayEquals(new int[] {100 - 5120, 100 + 5120}, JourneyMapFold.viewSpan(100.0, 1280, 64),
                "1280 px at 64 px per region is 10240 blocks, 5120 each side of the center at 100");
        assertArrayEquals(new int[] {(int) Math.floor(100.5 - 640), (int) Math.ceil(100.5 + 640)},
                JourneyMapFold.viewSpan(100.5, 1280, 512), "a fractional center does not floor and ceil outward");
    }
}
