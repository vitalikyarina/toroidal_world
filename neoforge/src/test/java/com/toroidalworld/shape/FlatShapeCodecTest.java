package com.toroidalworld.shape;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;
import com.toroidalworld.shape.FlatShape.Identification;
import com.toroidalworld.shape.FlatShape.Mirror;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import net.minecraft.core.Direction;

class FlatShapeCodecTest {
    private static final WorldLoopBounds SQUARE = WorldLoopBounds.ofWidth(32);
    private static final WorldLoopBounds X_ONLY =
            new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE);
    private static final WorldLoopBounds Z_ONLY =
            new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, new AxisBounds.Looped(-16, 16));

    private static List<FlatShape> everyIdentification() {
        return List.of(
                FlatShape.rectangle(),
                FlatShape.cylinder(X_ONLY),
                FlatShape.latticeTorus(SQUARE, 0),
                FlatShape.latticeTorus(SQUARE, 5),
                FlatShape.mirrored(Z_ONLY, Direction.Axis.X, 3),
                FlatShape.mirrored(SQUARE, Direction.Axis.Z, -7));
    }

    @Test
    void theFixtureCoversAllFiveIdentifications() {
        assertEquals(
                List.of(Identification.RECTANGLE, Identification.CYLINDER, Identification.LATTICE_TORUS,
                        Identification.LATTICE_TORUS, Identification.MOBIUS, Identification.KLEIN),
                everyIdentification().stream().map(FlatShape::identification).toList());
    }

    @Test
    void everyIdentificationRoundTripsThroughItsOwnWriting() {
        for (FlatShape shape : everyIdentification()) {
            JsonElement written = write(shape);
            assertEquals(shape, FlatShape.CODEC.parse(JsonOps.INSTANCE, written).getOrThrow(), written.toString());
        }
    }

    @Test
    void anUnskewedUnmirroredShapeWritesExactlyTheLegacyBoundsForm() {
        for (WorldLoopBounds bounds : List.of(SQUARE, X_ONLY, Z_ONLY, WorldLoopBounds.UNBOUNDED)) {
            assertEquals(WorldLoopBounds.CODEC.encodeStart(JsonOps.INSTANCE, bounds).getOrThrow(),
                    write(new FlatShape(bounds, 0, null)),
                    bounds.toString());
        }
    }

    @Test
    void aSkewOrAMirrorAddsItsOwnKey() {
        assertEquals(
                JsonParser.parseString("{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},"
                        + "\"z\":{\"min_chunk\":-16,\"max_chunk\":16},\"skew_chunks\":5}"),
                write(FlatShape.latticeTorus(SQUARE, 5)));
        assertEquals(
                JsonParser.parseString("{\"x\":{},\"z\":{\"min_chunk\":-16,\"max_chunk\":16},"
                        + "\"mirror\":{\"axis\":\"x\",\"line_chunk\":3}}"),
                write(FlatShape.mirrored(Z_ONLY, Direction.Axis.X, 3)));
    }

    @Test
    void theLegacyBoundsFormReadsBackAsAnUnskewedUnmirroredShape() {
        FlatShape read = FlatShape.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},"
                        + "\"z\":{\"min_chunk\":-16,\"max_chunk\":16}}")).getOrThrow();

        assertEquals(new FlatShape(SQUARE, 0, null), read);
        assertTrue(read.decomposesPerAxis());
    }

    @Test
    void aSkewNormalisesIntoHalfAWorldOnRead() {
        FlatShape read = FlatShape.CODEC.parse(JsonOps.INSTANCE,
                JsonParser.parseString("{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},"
                        + "\"z\":{\"min_chunk\":-16,\"max_chunk\":16},\"skew_chunks\":37}")).getOrThrow();

        assertEquals(5, read.skewChunks());
        assertEquals(read, FlatShape.CODEC.parse(JsonOps.INSTANCE, write(read)).getOrThrow());
    }

    @Test
    void codecRejectsASkewedMirror() {
        assertTrue(readError("{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},"
                + "\"z\":{\"min_chunk\":-16,\"max_chunk\":16},\"skew_chunks\":5,"
                + "\"mirror\":{\"axis\":\"x\",\"line_chunk\":0}}")
                .contains("skewed mirror"));
    }

    @Test
    void codecRejectsASkewWithoutBothAxesLooped() {
        assertTrue(readError("{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},\"z\":{},\"skew_chunks\":5}")
                .contains("skewed lattice needs both axes looped"));
    }

    @Test
    void codecRejectsAMirrorWhoseGlideAxisDoesNotLoop() {
        assertTrue(readError("{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},\"z\":{},"
                + "\"mirror\":{\"axis\":\"x\",\"line_chunk\":0}}")
                .contains("needs the axis it glides along to loop"));
    }

    @Test
    void codecRejectsAMirrorOnY() {
        assertTrue(readError("{\"x\":{\"min_chunk\":-16,\"max_chunk\":16},"
                + "\"z\":{\"min_chunk\":-16,\"max_chunk\":16},"
                + "\"mirror\":{\"axis\":\"y\",\"line_chunk\":0}}")
                .contains("never mirrors Y"));
    }

    @Test
    void everyIdentificationRoundTripsOnTheWireAndDrainsTheBuffer() {
        for (FlatShape shape : everyIdentification()) {
            ByteBuf buffer = Unpooled.buffer();
            FlatShape.STREAM_CODEC.encode(buffer, shape);

            assertEquals(shape, FlatShape.STREAM_CODEC.decode(buffer), shape.toString());
            assertEquals(0, buffer.readableBytes(), shape.toString());
        }
    }

    @Test
    void theWireRefusesAnUnknownMirrorAxisId() {
        ByteBuf buffer = Unpooled.buffer();
        FlatShape.STREAM_CODEC.encode(buffer, FlatShape.mirrored(Z_ONLY, Direction.Axis.X, 3));
        int mirrorAxisIndex = buffer.readableBytes() - 2;
        buffer.setByte(mirrorAxisIndex, 7);

        assertThrows(DecoderException.class, () -> FlatShape.STREAM_CODEC.decode(buffer));
    }

    @Test
    void mirrorOnYIsRejectedByItsOwnConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new Mirror(Direction.Axis.Y, 0));
    }

    private static JsonElement write(FlatShape shape) {
        return FlatShape.CODEC.encodeStart(JsonOps.INSTANCE, shape).getOrThrow();
    }

    private static String readError(String json) {
        DataResult<FlatShape> result = FlatShape.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(json));
        assertTrue(result.isError(), result.toString());
        return result.error().orElseThrow().message();
    }
}
