package com.toroidalworld.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.toroidalworld.compat.AxisCopies;

class XaeroWorldMapFoldSeamTest {
    @Test
    void aRegionInsideAnAlignedWorldStaysCanonical() {
        AxisCopies axis = AxisCopies.looped(-512, 1024);
        assertFalse(XaeroWorldMapFold.regionCrossesSeam(axis, -1), "region -1 covers -512..-1, the world's first half");
        assertFalse(XaeroWorldMapFold.regionCrossesSeam(axis, 0), "region 0 covers 0..511, the world's second half");
        assertTrue(XaeroWorldMapFold.regionCrossesSeam(axis, 1), "region 1 lies past the seam at 512");
        assertTrue(XaeroWorldMapFold.regionCrossesSeam(axis, -2), "region -2 lies before the seam at -512");
    }

    @Test
    void everyRegionOfAHalfShiftedWorldCrossesASeam() {
        AxisCopies axis = AxisCopies.looped(-256, 512);
        assertTrue(XaeroWorldMapFold.regionCrossesSeam(axis, 0), "region 0 spans 0..511 against a world ending at 255");
        assertTrue(XaeroWorldMapFold.regionCrossesSeam(axis, -1), "region -1 spans -512..-1 against a world starting at -256");
    }

    @Test
    void aRegionWiderThanTheWorldCrossesASeam() {
        assertTrue(XaeroWorldMapFold.regionCrossesSeam(AxisCopies.looped(-128, 256), 0),
                "a 256-block world fits twice into region 0");
    }

    @Test
    void anUnloopedAxisNeverCrosses() {
        assertFalse(XaeroWorldMapFold.regionCrossesSeam(AxisCopies.UNBOUNDED, 7), "an unbounded axis has no seam");
    }
}
