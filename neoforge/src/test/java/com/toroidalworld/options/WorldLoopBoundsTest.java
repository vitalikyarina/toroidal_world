package com.toroidalworld.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

// The two guards persisted bounds pass through. The codec is the gate every saved world enters by: it must accept
// exactly the three written shapes — a real span, an absent pair, the legacy sentinel — and refuse a degenerate span
// before it can become a WrapDomain with no positive width. isSquare is the claim guard of the Re-Create restore
// path: it must accept exactly the shapes the creation flow builds, and refuse every codec-valid shape the settings
// screen cannot represent — those are the ones whose chunkWidth() would throw or lie.
class WorldLoopBoundsTest {
    // The sentinel the retired model wrote for a non-looping axis: 1883191 chunks, kept as the frozen literal rather
    // than re-derived from the constants it once came from. The number already sits in legacy files, so the codec owes
    // acceptance of exactly this value forever — and if the game constants behind the production copy ever move, the
    // derivation drifts away from the files while a test deriving the same way would drift with it and stay green.
    private static final int LEGACY_DISABLED_AXIS_RADIUS = 1883191;

    @Test
    void codecAcceptsARealSpan() {
        assertEquals(new AxisBounds.Looped(-32, 32), readAxis("{\"min_chunk\":-32,\"max_chunk\":32}"));
    }

    @Test
    void codecAcceptsAnAbsentPairAsUnbounded() {
        assertEquals(AxisBounds.Unbounded.INSTANCE, readAxis("{}"));
    }

    @Test
    void codecAcceptsTheLegacySentinelAsUnbounded() {
        int radius = LEGACY_DISABLED_AXIS_RADIUS;
        assertEquals(AxisBounds.Unbounded.INSTANCE,
                readAxis("{\"min_chunk\":" + -radius + ",\"max_chunk\":" + radius + "}"));
        assertEquals(AxisBounds.Unbounded.INSTANCE,
                readAxis("{\"min_chunk\":" + -radius + ",\"max_chunk\":16}"));
        assertEquals(AxisBounds.Unbounded.INSTANCE,
                readAxis("{\"min_chunk\":-16,\"max_chunk\":" + radius + "}"));
    }

    @Test
    void codecRejectsAZeroWidthSpan() {
        String error = readAxisError("{\"min_chunk\":5,\"max_chunk\":5}");
        assertTrue(error.contains("[5, 5)"), error);
    }

    @Test
    void codecRejectsANegativeWidthSpan() {
        String error = readAxisError("{\"min_chunk\":10,\"max_chunk\":-10}");
        assertTrue(error.contains("[10, -10)"), error);
    }

    @Test
    void codecRejectsAOneSidedPair() {
        String error = readAxisError("{\"min_chunk\":0}");
        assertTrue(error.contains("both chunk bounds or neither"), error);
    }

    // The write path is what every one of the accepted shapes above comes back through on the next save: a looped span
    // keeps its two bounds, an unbounded axis goes out as the absent pair — never the sentinel it may have been read
    // from.
    @Test
    void codecWritesALoopedSpanAsItsTwoBounds() {
        assertEquals(JsonParser.parseString("{\"min_chunk\":-32,\"max_chunk\":32}"),
                writeAxis(new AxisBounds.Looped(-32, 32)));
    }

    @Test
    void codecWritesAnUnboundedAxisAsAnAbsentPair() {
        assertEquals(JsonParser.parseString("{}"), writeAxis(AxisBounds.Unbounded.INSTANCE));
    }

    @Test
    void axisRoundTripsThroughItsOwnWriting() {
        List<AxisBounds> axes = List.of(
                new AxisBounds.Looped(-32, 32),
                new AxisBounds.Looped(0, 64),
                new AxisBounds.Looped(-48, 16),
                AxisBounds.Unbounded.INSTANCE);
        for (AxisBounds axis : axes) {
            assertEquals(axis, AxisBounds.CODEC.parse(JsonOps.INSTANCE, writeAxis(axis)).getOrThrow());
        }
    }

    @Test
    void worldBoundsWriteUnderTheirAxisKeysAndRoundTrip() {
        WorldLoopBounds mixed = new WorldLoopBounds(new AxisBounds.Looped(-16, 48), AxisBounds.Unbounded.INSTANCE);
        JsonElement written = WorldLoopBounds.CODEC.encodeStart(JsonOps.INSTANCE, mixed).getOrThrow();

        assertEquals(JsonParser.parseString("{\"x\":{\"min_chunk\":-16,\"max_chunk\":48},\"z\":{}}"), written);
        assertEquals(mixed, WorldLoopBounds.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow());
    }

    @Test
    void worldBoundsRoundTripEveryAxisShape() {
        List<WorldLoopBounds> shapes = List.of(
                WorldLoopBounds.ofWidth(32),
                WorldLoopBounds.ofWidth(5),
                WorldLoopBounds.UNBOUNDED,
                new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, new AxisBounds.Looped(-16, 16)),
                new WorldLoopBounds(-16, 16, -32, 32));
        for (WorldLoopBounds bounds : shapes) {
            JsonElement written = WorldLoopBounds.CODEC.encodeStart(JsonOps.INSTANCE, bounds).getOrThrow();
            assertEquals(bounds, WorldLoopBounds.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow(),
                    written.toString());
        }
    }

    private static JsonElement writeAxis(AxisBounds axis) {
        return AxisBounds.CODEC.encodeStart(JsonOps.INSTANCE, axis).getOrThrow();
    }

    private static AxisBounds readAxis(String json) {
        return AxisBounds.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow();
    }

    private static String readAxisError(String json) {
        DataResult<AxisBounds> result = AxisBounds.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
        assertTrue(result.isError());
        return result.error().orElseThrow().message();
    }
    @Test
    void aCreationFlowWorldIsSquare() {
        assertTrue(WorldLoopBounds.ofWidth(32).isSquare());
        assertTrue(WorldLoopBounds.ofWidth(5).isSquare());
    }

    @Test
    void equalWidthsPlacedDifferentlyPerAxisAreStillSquare() {
        assertTrue(new WorldLoopBounds(-48, 16, 0, 64).isSquare());
    }

    @Test
    void aSingleAxisLoopIsNotSquare() {
        assertFalse(new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE).isSquare());
        assertFalse(new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, new AxisBounds.Looped(-16, 16)).isSquare());
        assertFalse(WorldLoopBounds.UNBOUNDED.isSquare());
    }

    @Test
    void aRectangularWorldIsNotSquare() {
        assertFalse(new WorldLoopBounds(-16, 16, -32, 32).isSquare());
    }

    @Test
    void aDegenerateSpanIsNotSquare() {
        assertFalse(new WorldLoopBounds(5, 5, 5, 5).isSquare());
        assertFalse(new WorldLoopBounds(10, -10, 10, -10).isSquare());
    }
}
