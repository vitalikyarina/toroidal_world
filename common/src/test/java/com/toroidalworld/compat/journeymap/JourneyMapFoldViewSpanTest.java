package com.toroidalworld.compat.journeymap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class JourneyMapFoldViewSpanTest {
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
