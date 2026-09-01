package com.toroidalworld.options;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import net.minecraft.core.Direction;

class WorldLoopBoundsTest {
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
    void aLoopedAxisAnswersBlockQueriesFromItsChunkBounds() {
        AxisBounds.Looped axis = new AxisBounds.Looped(-32, 32);

        assertEquals(-32 * 16, axis.minBlock());
        assertEquals(32 * 16, axis.maxBlock());
        assertEquals(64 * 16, axis.blockWidth());

        assertFalse(axis.isOver(-32 * 16));
        assertFalse(axis.isOver(32 * 16 - 0.5));
        assertTrue(axis.isOver(32 * 16));
        assertTrue(axis.isOver(-32 * 16 - 1));

        assertTrue(axis.fitsInHalf(32 * 16));
        assertFalse(axis.fitsInHalf(32 * 16 + 0.5));
        assertTrue(axis.coversWorld(64 * 16));
        assertFalse(axis.coversWorld(64 * 16 - 1));
        assertFalse(axis.foldsOntoItself(64));
        assertTrue(axis.foldsOntoItself(65));
    }

    @Test
    void anUnboundedAxisIsNeverOverNeverCoveredAndAlwaysFitsInHalf() {
        AxisBounds axis = AxisBounds.Unbounded.INSTANCE;

        assertFalse(axis.isOver(-1.0e9));
        assertFalse(axis.isOver(1.0e9));
        assertTrue(axis.fitsInHalf(1.0e9));
        assertFalse(axis.coversWorld(1.0e9));
        assertFalse(axis.foldsOntoItself(Integer.MAX_VALUE));
    }

    @Test
    void maxViewDistanceIsHalfTheNarrowerLoopedWidthMinusThreeAndNeverBelowOne() {
        assertEquals(64 / 2 - 3, WorldLoopBounds.ofWidth(64).maxViewDistance());
        assertEquals(16 / 2 - 3, new WorldLoopBounds(-48, 16, 0, 16).maxViewDistance());
        assertEquals(1, new WorldLoopBounds(-2, 3, -2, 3).maxViewDistance());
        assertEquals(64 / 2 - 3, new WorldLoopBounds(
                new AxisBounds.Looped(-32, 32), AxisBounds.Unbounded.INSTANCE).maxViewDistance());
        assertEquals(Integer.MAX_VALUE, WorldLoopBounds.UNBOUNDED.maxViewDistance());
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

    @Test
    void ofWidthOnOneAxisLoopsThatAxisCentredAndLeavesTheOtherUnbounded() {
        assertEquals(new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE),
                WorldLoopBounds.ofWidth(Direction.Axis.X, 32));
        assertEquals(new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, new AxisBounds.Looped(-2, 3)),
                WorldLoopBounds.ofWidth(Direction.Axis.Z, 5));
        assertThrows(IllegalArgumentException.class, () -> WorldLoopBounds.ofWidth(Direction.Axis.Y, 32));
    }

    @Test
    void perAxisReadsAnswerForTheAxisNamed() {
        WorldLoopBounds xOnly = WorldLoopBounds.ofWidth(Direction.Axis.X, 32);

        assertEquals(new AxisBounds.Looped(-16, 16), xOnly.axis(Direction.Axis.X));
        assertEquals(AxisBounds.Unbounded.INSTANCE, xOnly.axis(Direction.Axis.Z));
        assertTrue(xOnly.loops(Direction.Axis.X));
        assertFalse(xOnly.loops(Direction.Axis.Z));
        assertEquals(32, xOnly.chunkWidth(Direction.Axis.X));
        assertThrows(IllegalStateException.class, () -> xOnly.chunkWidth(Direction.Axis.Z));
        assertThrows(IllegalArgumentException.class, () -> xOnly.axis(Direction.Axis.Y));

        WorldLoopBounds rectangle = new WorldLoopBounds(-16, 16, -32, 32);
        assertEquals(32, rectangle.chunkWidth(Direction.Axis.X));
        assertEquals(64, rectangle.chunkWidth(Direction.Axis.Z));
    }

    @Test
    void scaledDownDividesEveryLoopedWidthRecentredAndLeavesAnUnboundedAxisAlone() {
        assertEquals(WorldLoopBounds.ofWidth(16), WorldLoopBounds.ofWidth(32).scaledDown(2));
        assertEquals(WorldLoopBounds.ofWidth(32), new WorldLoopBounds(-48, 16, 0, 64).scaledDown(2));
        assertEquals(WorldLoopBounds.ofWidth(Direction.Axis.Z, 16),
                WorldLoopBounds.ofWidth(Direction.Axis.Z, 128).scaledDown(8));
        assertEquals(new WorldLoopBounds(-8, 8, -32, 32), new WorldLoopBounds(-16, 16, -64, 64).scaledDown(2));
        assertEquals(WorldLoopBounds.UNBOUNDED, WorldLoopBounds.UNBOUNDED.scaledDown(8));
        assertEquals(WorldLoopBounds.ofWidth(32), WorldLoopBounds.ofWidth(32).scaledDown(1));
    }
}
