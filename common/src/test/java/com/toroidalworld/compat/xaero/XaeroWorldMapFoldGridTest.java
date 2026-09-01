package com.toroidalworld.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class XaeroWorldMapFoldGridTest {
    @Test
    void theViewSpanIsTheWindowInBlocksAroundTheCameraPlusAMargin() {
        assertArrayEquals(new int[] {100 - 1920 - 2, 100 + 1920 + 2},
                XaeroWorldMapFold.viewSpan(100.0, 1920, 0.5, 2),
                "1920 px at 0.5 px per block is 3840 blocks, 1920 each side of the camera, plus the margin");
        assertArrayEquals(new int[] {(int) Math.floor(100.5 - 1920) - 2, (int) Math.ceil(100.5 + 1920) + 2},
                XaeroWorldMapFold.viewSpan(100.5, 1920, 0.5, 2),
                "a fractional camera does not floor and ceil outward");
    }
}
