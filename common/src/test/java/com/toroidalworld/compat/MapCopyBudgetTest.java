package com.toroidalworld.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.toroidalworld.engine.seam.MapSurfaceCopies.Copies;

class MapCopyBudgetTest {
    private static final int BLIT_BUDGET = 16384;

    @Test
    void theCapSpendsTheBlitBudgetOverTheTilesWithContent() {
        assertEquals(63, MapCopyBudget.copyRangeCap(2, 1), "one tile on a torus: (sqrt(16384) - 1) / 2 = 63");
        assertEquals(8191, MapCopyBudget.copyRangeCap(1, 1), "one tile on a cylinder: (16384 - 1) / 2 = 8191");
        assertEquals(15, MapCopyBudget.copyRangeCap(2, 16), "16 tiles on a torus: (sqrt(1024) - 1) / 2 = 15");
        assertEquals(511, MapCopyBudget.copyRangeCap(1, 16), "16 tiles on a cylinder: (1024 - 1) / 2 = 511");
        assertEquals(0, MapCopyBudget.copyRangeCap(2, BLIT_BUDGET / 4), "4 blits per tile leave no torus copy");
        assertEquals(1, MapCopyBudget.copyRangeCap(1, BLIT_BUDGET / 4), "4 blits per tile leave one cylinder copy per side");
        assertEquals(0, MapCopyBudget.copyRangeCap(0, 1), "no looped axis got copies");
    }

    @Test
    void aViewportCoveredByThreeQuartersNeedsThatManyCopies() {
        assertEquals(3, MapCopyBudget.copyRange(1, 1, 128.0, 512), "ceil(512 * 0.75 / 128) is 3");
        assertEquals(3, MapCopyBudget.copyRange(2, 1, 128.0, 512), "the torus reads a different count under its cap");
    }

    @Test
    void theCapBindsWhenTheViewportAsksForMore() {
        assertEquals(63, MapCopyBudget.copyRange(2, 1, 16.0, 1920), "ceil(1920 * 0.75 / 16) = 90 was not capped at 63");
        assertEquals(90, MapCopyBudget.copyRange(1, 1, 16.0, 1920), "90 copies on a one-tile cylinder were capped");
        assertEquals(15, MapCopyBudget.copyRange(2, 16, 16.0, 1920), "90 copies over 16 torus tiles were not capped at 15");
    }

    @Test
    void anAxisWithNoPeriodDrawsNoCopies() {
        assertEquals(0, MapCopyBudget.copyRange(1, 1, 0.0, 1920), "an unbounded axis got copies");
    }

    @Test
    void aViewportCountsTheTilesAcrossIt() {
        assertEquals(6, MapCopyBudget.viewportTiles(256, 1280), "ceil(1280 / 256) + 1 is 6");
        assertEquals(161, MapCopyBudget.viewportTiles(8, 1280), "ceil(1280 / 8) + 1 is 161");
        assertEquals(1, MapCopyBudget.viewportTiles(0, 1280), "a zero tile size is not one tile");
    }

    @Test
    void thePaintedBoxSpansEveryCopyDrawn() {
        Copies copies = MapCopyBudget.painted(AxisCopies.looped(0, 512), 1, AxisCopies.looped(0, 512), 2);
        assertEquals(2, copies.reach(), "the reach is the wider of the two ranges");
        assertEquals(-512, copies.painted().minX(), "one copy left of a world starting at 0");
        assertEquals(1023, copies.painted().maxX(), "one copy right of a 512-block world, last block inclusive");
        assertEquals(-1024, copies.painted().minZ(), "two copies below");
        assertEquals(1535, copies.painted().maxZ(), "two copies above");
    }

    @Test
    void anUnboundedAxisPaintsEverywhere() {
        Copies copies = MapCopyBudget.painted(AxisCopies.UNBOUNDED, 0, AxisCopies.looped(0, 512), 0);
        assertEquals(Integer.MIN_VALUE, copies.painted().minX(), "an unbounded axis was bounded");
        assertEquals(Integer.MAX_VALUE, copies.painted().maxX(), "an unbounded axis was bounded");
        assertEquals(0, copies.painted().minZ(), "the looped axis kept its own bounds");
        assertEquals(511, copies.painted().maxZ(), "the looped axis kept its own bounds");
    }
}
