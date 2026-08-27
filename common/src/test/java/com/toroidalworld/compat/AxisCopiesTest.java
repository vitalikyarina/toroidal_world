package com.toroidalworld.compat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AxisCopiesTest {
    private static final int MIN = -512;
    private static final int WIDTH = 1024;

    @Test
    void aLoopedAxisNamesItsBoundsAndOffsets() {
        AxisCopies axis = AxisCopies.looped(MIN, WIDTH);
        assertTrue(axis.loops(), "the looped axis reads as unbounded");
        assertEquals(MIN, axis.min(), "the first block moved");
        assertEquals(MIN + WIDTH, axis.max(), "the exclusive bound is not min + width");
        assertEquals(-WIDTH, axis.offset(-1), "the lap before the world is not a width back");
        assertEquals(0, axis.offset(0), "the canonical lap is offset");
        assertEquals(WIDTH, axis.offset(1), "the lap after the world is not a width on");
    }

    @Test
    void theLapsAreTheCopiesTouchingTheSpan() {
        AxisCopies axis = AxisCopies.looped(MIN, WIDTH);
        assertArrayEquals(new int[] {0}, axis.laps(MIN, MIN + WIDTH), "the canonical world alone is not one lap");
        assertArrayEquals(new int[] {-1, 0, 1}, axis.laps(MIN - WIDTH, MIN + 2 * WIDTH),
                "three worlds are not three laps");
        assertArrayEquals(new int[] {0, 1}, axis.laps(500, 600), "a span across the seam at 512 does not touch laps 0 and 1");
        assertArrayEquals(new int[] {3}, axis.laps(3000, 3100), "3000..3100 lies in lap floor((3000 + 512) / 1024) = 3");
        assertArrayEquals(new int[] {-2, -1}, axis.laps(-2000, -1000), "-2000..-1000 spans laps -2 and -1");
    }

    @Test
    void theSeamsOutlineEveryCopyInTheSpan() {
        AxisCopies axis = AxisCopies.looped(MIN, WIDTH);
        assertArrayEquals(new int[] {MIN - WIDTH, MIN, MIN + WIDTH, MIN + 2 * WIDTH},
                axis.seams(MIN - WIDTH, MIN + 2 * WIDTH), "three copies are not outlined by four seams");
        assertArrayEquals(new int[] {MIN, MIN + WIDTH}, axis.seams(-100, 100),
                "a span inside the world is not outlined by its two seams");
        assertArrayEquals(new int[] {MIN + 3 * WIDTH, MIN + 4 * WIDTH, MIN + 5 * WIDTH}, axis.seams(3000, 4200),
                "3000..4200 touches laps 3 and 4: three seams");
    }

    @Test
    void theReachIsHowFarFromTheCanonicalCopyTheSpanGoes() {
        AxisCopies axis = AxisCopies.looped(MIN, WIDTH);
        assertEquals(0, axis.reach(-100, 100), "a span inside the world reaches past the canonical copy");
        assertEquals(1, axis.reach(500, 600), "a span across the seam does not reach one copy out");
        assertEquals(3, axis.reach(MIN - 3 * WIDTH, MIN + 4 * WIDTH), "seven worlds do not reach three copies each side");
        assertEquals(4, axis.reach(3000, 4200), "3000..4200 touches lap 4 and does not reach it");
        assertEquals(0, AxisCopies.UNBOUNDED.reach(-40000000, 40000000), "an unbounded axis reaches past its one copy");
    }

    @Test
    void anUnboundedAxisDrawsTheOneLapWithNoSeamAndNoOffset() {
        AxisCopies axis = AxisCopies.UNBOUNDED;
        assertFalse(axis.loops(), "the unbounded axis reads as looped");
        assertArrayEquals(new int[] {0}, axis.laps(-40000000, 40000000), "an unbounded axis has copies");
        assertArrayEquals(new int[0], axis.seams(-40000000, 40000000), "an unbounded axis has a seam");
        assertEquals(0, axis.offset(-1), "an unbounded axis offsets a lap");
        assertEquals(0, axis.offset(1), "an unbounded axis offsets a lap");
    }

    @Test
    void aLoopedAxisClipsASpanToTheWorld() {
        AxisCopies axis = AxisCopies.looped(MIN, WIDTH);
        assertEquals(MIN, axis.clipMin(MIN - 100), "a span starting before the world was not clipped to min");
        assertEquals(MIN + 100, axis.clipMin(MIN + 100), "a span starting inside the world was moved");
        assertEquals(MIN + WIDTH, axis.clipMax(MIN + WIDTH + 100), "a span ending past the world was not clipped to max");
        assertEquals(MIN + WIDTH - 100, axis.clipMax(MIN + WIDTH - 100), "a span ending inside the world was moved");
    }

    @Test
    void anUnboundedAxisClipsNothing() {
        AxisCopies axis = AxisCopies.UNBOUNDED;
        assertEquals(-40000000, axis.clipMin(-40000000), "an unbounded axis clipped a span start");
        assertEquals(40000000, axis.clipMax(40000000), "an unbounded axis clipped a span end");
    }

    @Test
    void anUnboundedAxisHasNoBounds() {
        AxisCopies axis = AxisCopies.UNBOUNDED;
        assertThrows(IllegalStateException.class, axis::min, "min answered on an unbounded axis");
        assertThrows(IllegalStateException.class, axis::max, "max answered on an unbounded axis");
    }

    @Test
    void aLoopedAxisNeedsAWidth() {
        assertThrows(IllegalArgumentException.class, () -> AxisCopies.looped(MIN, 0), "a zero width was accepted");
    }
}
