package com.toroidalworld.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class AxisCopiesTest {
    private static final int MIN = -512;
    private static final int WIDTH = 1024;

    @Test
    void aLoopedAxisDrawsThreeLapsAPeriodApart() {
        AxisCopies axis = AxisCopies.looped(MIN, WIDTH);
        assertTrue(axis.loops(), "the looped axis reads as unbounded");
        assertEquals(List.of(-1, 0, 1), axis.laps(), "the laps are not the canonical world and one copy per side");
        assertEquals(MIN, axis.min(), "the first block moved");
        assertEquals(MIN + WIDTH, axis.max(), "the exclusive bound is not min + width");
        assertEquals(-WIDTH, axis.offset(-1), "the lap before the world is not a width back");
        assertEquals(0, axis.offset(0), "the canonical lap is offset");
        assertEquals(WIDTH, axis.offset(1), "the lap after the world is not a width on");
    }

    @Test
    void anUnboundedAxisDrawsTheOneLapWithNoOffset() {
        AxisCopies axis = AxisCopies.UNBOUNDED;
        assertFalse(axis.loops(), "the unbounded axis reads as looped");
        assertEquals(List.of(0), axis.laps(), "an unbounded axis has copies");
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
