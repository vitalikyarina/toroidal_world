package com.toroidalworld.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.toroidalworld.options.WorldLoopBounds;
import com.toroidalworld.options.WorldLoopBounds.AxisBounds;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;

// The payload is the client's only source of a level's wrap bounds, so the wire format is a contract: every axis shape
// the bounds model allows must come back exactly, and reading must consume exactly what writing produced — a codec
// that leaves bytes behind desyncs every payload after it in the buffer. The round trip alone cannot see the two
// halves drift together — a codec that consistently swapped its two bounds would round-trip clean — so one looped
// shape is also pinned to the literal bytes it owes the wire.
class WrappingSettingsPayloadTest {
    private static final RegistryAccess.Frozen REGISTRIES =
            RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);

    private static RegistryFriendlyByteBuf buffer() {
        return new RegistryFriendlyByteBuf(Unpooled.buffer(), REGISTRIES);
    }

    @Test
    void everyAxisShapeRoundTripsAndDrainsTheBuffer() {
        List<WorldLoopBounds> shapes = List.of(
                new WorldLoopBounds(-32, 32, -32, 32),
                new WorldLoopBounds(-48, 16, 0, 64),
                new WorldLoopBounds(-4096, 4096, -4096, 4096),
                WorldLoopBounds.UNBOUNDED,
                new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE),
                new WorldLoopBounds(AxisBounds.Unbounded.INSTANCE, new AxisBounds.Looped(-16, 16)));
        for (WorldLoopBounds bounds : shapes) {
            RegistryFriendlyByteBuf buffer = buffer();
            WrappingSettingsPayload.STREAM_CODEC.encode(buffer, new WrappingSettingsPayload(bounds));

            assertEquals(bounds, WrappingSettingsPayload.STREAM_CODEC.decode(buffer).bounds(), bounds.toString());
            assertEquals(0, buffer.readableBytes(), bounds.toString());
        }
    }

    // Hand-assembled from the declared layout: a looped flag byte, then min and max chunk as protocol VarInts (7 data
    // bits per byte, low bits first, high bit as continuation) — so -16 is the five-byte F0 FF FF FF 0F and 16 is the
    // single byte 10. Asserted in both directions against the same bytes: encode must lay down exactly these, decode
    // must read exactly these back, so encode and decode drifting together fails instead of round-tripping clean.
    @Test
    void aLoopedAxisLaysDownItsExactBytes() {
        WorldLoopBounds bounds =
                new WorldLoopBounds(new AxisBounds.Looped(-16, 16), AxisBounds.Unbounded.INSTANCE);
        byte[] wire = {
                0x01, (byte) 0xF0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x0F, 0x10,
                0x00};

        RegistryFriendlyByteBuf encoded = buffer();
        WrappingSettingsPayload.STREAM_CODEC.encode(encoded, new WrappingSettingsPayload(bounds));
        byte[] written = new byte[encoded.readableBytes()];
        encoded.readBytes(written);
        assertArrayEquals(wire, written);

        RegistryFriendlyByteBuf incoming = buffer();
        incoming.writeBytes(wire);
        assertEquals(bounds, WrappingSettingsPayload.STREAM_CODEC.decode(incoming).bounds());
        assertEquals(0, incoming.readableBytes());
    }

    @Test
    void anUnboundedAxisIsASingleByteOnTheWire() {
        RegistryFriendlyByteBuf buffer = buffer();
        WrappingSettingsPayload.STREAM_CODEC.encode(buffer, new WrappingSettingsPayload(WorldLoopBounds.UNBOUNDED));

        assertEquals(2, buffer.readableBytes());
    }
}
