package com.toroidalworld.compat.xaero;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

import com.toroidalworld.compat.AxisCopies;

class XaeroWorldMapFoldGridTest {
    private static final int MIN = -512;
    private static final int WIDTH = 1024;
    private static final AxisCopies LOOPED = AxisCopies.looped(MIN, WIDTH);

    @Test
    void aLoopedAxisOutlinesTheThreeCopies() {
        assertArrayEquals(new int[] {MIN - WIDTH, MIN, MIN + WIDTH, MIN + 2 * WIDTH},
                XaeroWorldMapFold.gridLines(LOOPED), "the four edges of the canonical world and its two copies moved");
    }

    @Test
    void anUnboundedAxisHasNoSeamLine() {
        assertArrayEquals(new int[0], XaeroWorldMapFold.gridLines(AxisCopies.UNBOUNDED), "an unbounded axis got a seam line");
    }

    @Test
    void aLoopedAxisSpansTheThreeCopies() {
        assertArrayEquals(new int[] {MIN - WIDTH, MIN + 2 * WIDTH},
                XaeroWorldMapFold.gridExtent(LOOPED, 12345.0, 1920, 0.5, 2),
                "the looped extent read the viewport instead of the copies");
    }

    @Test
    void anUnboundedAxisSpansTheViewportPlusAMargin() {
        assertArrayEquals(new int[] {100 - 1920 - 2, 100 + 1920 + 2},
                XaeroWorldMapFold.gridExtent(AxisCopies.UNBOUNDED, 100.0, 1920, 0.5, 2),
                "1920 px at 0.5 px per block is 3840 blocks, 1920 each side of the camera, plus the margin");
        assertArrayEquals(new int[] {(int) Math.floor(100.5 - 1920) - 2, (int) Math.ceil(100.5 + 1920) + 2},
                XaeroWorldMapFold.gridExtent(AxisCopies.UNBOUNDED, 100.5, 1920, 0.5, 2),
                "a fractional camera does not floor and ceil outward");
    }
}
